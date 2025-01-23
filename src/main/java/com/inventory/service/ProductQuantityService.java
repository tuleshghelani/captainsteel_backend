package com.inventory.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.entity.Product;
import com.inventory.exception.ValidationException;
import com.inventory.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductQuantityService {
    private final ProductRepository productRepository;
    private final ConcurrentHashMap<Long, Lock> productLocks = new ConcurrentHashMap<>();
    
    private Lock getProductLock(Long productId) {
        return productLocks.computeIfAbsent(productId, k -> new ReentrantLock());
    }
    
    @Transactional
    public void updateProductQuantity(Long productId, Long quantityChange, Boolean isPurchase, Boolean isSale, Boolean isBlock) {
        Lock lock = getProductLock(productId);
        lock.lock();
        try {
            Product product = getAndValidateProduct(productId);
            
            if (Boolean.TRUE.equals(isPurchase)) {
                handlePurchase(product, quantityChange);
            } else if (Boolean.TRUE.equals(isSale)) {
                handleSale(product, quantityChange);
            } else if (isBlock != null) {
                handleBlockUnblock(product, quantityChange, isBlock);
            }
            
            productRepository.save(product);
            logQuantityUpdate(product, quantityChange, isPurchase, isSale, isBlock);
            
        } catch (Exception e) {
            log.error("Error updating product quantity: {}", e.getMessage(), e);
            throw e;
        } finally {
            lock.unlock();
        }
    }
    
    private Product getAndValidateProduct(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ValidationException("Product not found"));
            
        if (product.getRemainingQuantity() == null) product.setRemainingQuantity(0L);
        if (product.getTotalRemainingQuantity() == null) product.setTotalRemainingQuantity(0L);
        if (product.getBlockedQuantity() == null) product.setBlockedQuantity(0L);
        
        return product;
    }
    
    private void handlePurchase(Product product, Long quantityChange) {
        Long newRemainingQuantity = product.getRemainingQuantity() + quantityChange;
        Long newTotalRemainingQuantity = product.getTotalRemainingQuantity() + quantityChange;
        
        validateNonNegativeQuantity(product, newRemainingQuantity, newTotalRemainingQuantity);
        
        product.setRemainingQuantity(newRemainingQuantity);
        product.setTotalRemainingQuantity(newTotalRemainingQuantity);
    }
    
    private void handleSale(Product product, Long quantityChange) {
        if (product.getRemainingQuantity() < quantityChange) {
            throw new ValidationException("Insufficient stock for product: " + product.getName());
        }
        
        Long newRemainingQuantity = product.getRemainingQuantity() - quantityChange;
        Long newTotalRemainingQuantity = product.getTotalRemainingQuantity() - quantityChange;
        
        validateNonNegativeQuantity(product, newRemainingQuantity, newTotalRemainingQuantity);
        
        product.setRemainingQuantity(newRemainingQuantity);
        product.setTotalRemainingQuantity(newTotalRemainingQuantity);
    }
    
    private void handleBlockUnblock(Product product, Long quantityChange, boolean isBlock) {
        if (isBlock && product.getRemainingQuantity() < quantityChange) {
            throw new ValidationException("Insufficient stock for product: " + product.getName());
        }
        
        Long newBlockedQuantity = product.getBlockedQuantity() + (isBlock ? quantityChange : -quantityChange);
        Long newRemainingQuantity = product.getRemainingQuantity() + (isBlock ? -quantityChange : quantityChange);
        
        if (newBlockedQuantity < 0) {
            throw new ValidationException("Operation would result in negative blocked quantity for product: " + product.getName());
        }
        
        validateNonNegativeQuantity(product, newRemainingQuantity, null);
        
        product.setBlockedQuantity(newBlockedQuantity);
        product.setRemainingQuantity(newRemainingQuantity);
    }
    
    private void validateNonNegativeQuantity(Product product, Long remaining, Long totalRemaining) {
        if (remaining != null && remaining < 0) {
            throw new ValidationException("Operation would result in negative stock for product: " + product.getName());
        }
        if (totalRemaining != null && totalRemaining < 0) {
            throw new ValidationException("Operation would result in negative total stock for product: " + product.getName());
        }
    }
    
    private void logQuantityUpdate(Product product, Long quantityChange, Boolean isPurchase, Boolean isSale, Boolean isBlock) {
        log.info("Product {} quantity updated. Change: {}, Purchase: {}, Sale: {}, Block: {}, Remaining: {}, Blocked: {}", 
            product.getId(), quantityChange, isPurchase, isSale, isBlock, 
            product.getRemainingQuantity(), product.getBlockedQuantity());
    }
}