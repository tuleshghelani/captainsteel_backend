package com.inventory.service;

import com.inventory.entity.Product;
import com.inventory.exception.ValidationException;
import com.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    public void updateProductQuantity(Long productId, Long quantityChange, boolean isPurchase, Boolean isBlock) {
        Lock lock = getProductLock(productId);
        lock.lock();
        try {
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ValidationException("Product not found"));
            
            // Initialize quantities if null
            if (product.getRemainingQuantity() == null) {
                product.setRemainingQuantity(0L);
            }
            if (product.getTotalRemainingQuantity() == null) {
                product.setTotalRemainingQuantity(0L);
            }
            if (product.getBlockedQuantity() == null) {
                product.setBlockedQuantity(0L);
            }
            
            // For sales, convert quantity to negative
            Long changeAmount = isPurchase ? quantityChange : -quantityChange;
            
            // Validate stock for sales
            if (!isPurchase && product.getRemainingQuantity() < quantityChange) {
                throw new ValidationException("Insufficient stock for product: " + product.getName());
            }
            
            // Handle blocked quantity
            if (isBlock != null) {
                Long newBlockedQuantity = product.getBlockedQuantity() + (isBlock ? quantityChange : -quantityChange);
                if (newBlockedQuantity < 0) {
                    throw new ValidationException("Operation would result in negative blocked quantity for product: " + product.getName());
                }
                product.setBlockedQuantity(newBlockedQuantity);
            }
            
            Long newRemainingQuantity = product.getRemainingQuantity() + changeAmount;
            Long newTotalRemainingQuantity = product.getTotalRemainingQuantity() + changeAmount;
            
            // Additional validation for negative quantities
            if (newRemainingQuantity < 0 || newTotalRemainingQuantity < 0) {
                throw new ValidationException("Operation would result in negative stock for product: " + product.getName());
            }
            
            product.setRemainingQuantity(newRemainingQuantity);
            if(isBlock == null) {
                product.setTotalRemainingQuantity(newTotalRemainingQuantity);
            }
            productRepository.save(product);
            
            log.info("Product {} quantity updated. Change: {}, IsPurchase: {}, IsBlock: {}, New Remaining: {}, Blocked: {}", 
                product.getId(), quantityChange, isPurchase, isBlock, newRemainingQuantity, product.getBlockedQuantity());
            
        } catch (Exception e) {
            log.error("Error updating product quantity: {}", e.getMessage(), e);
            throw e;
        } finally {
            lock.unlock();
        }
    }
}