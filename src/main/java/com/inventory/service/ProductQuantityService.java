package com.inventory.service;

import java.math.BigDecimal;
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
    public void updateProductQuantity(Long productId, BigDecimal quantityChange, Boolean isPurchase, Boolean isSale, Boolean isBlock) {
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
            
        if (product.getRemainingQuantity() == null) product.setRemainingQuantity(BigDecimal.ZERO);
        if (product.getTotalRemainingQuantity() == null) product.setTotalRemainingQuantity(BigDecimal.ZERO);
        if (product.getBlockedQuantity() == null) product.setBlockedQuantity(BigDecimal.ZERO);
        
        return product;
    }
    
    private void handlePurchase(Product product, BigDecimal quantityChange) {
        BigDecimal newRemainingQuantity = product.getRemainingQuantity().add(quantityChange);
        BigDecimal newTotalRemainingQuantity = product.getTotalRemainingQuantity().add(quantityChange);
        
        validateNonNegativeQuantity(product, newRemainingQuantity, newTotalRemainingQuantity);
        
        product.setRemainingQuantity(newRemainingQuantity);
        product.setTotalRemainingQuantity(newTotalRemainingQuantity);
    }
    
    private void handleSale(Product product, BigDecimal quantityChange) {
        if (product.getRemainingQuantity().compareTo(quantityChange) < 0) {
            throw new ValidationException("Insufficient stock for product: " + product.getName());
        }
        
        BigDecimal newRemainingQuantity = product.getRemainingQuantity().subtract(quantityChange);
        BigDecimal newTotalRemainingQuantity = product.getTotalRemainingQuantity().subtract(quantityChange);
        
        validateNonNegativeQuantity(product, newRemainingQuantity, newTotalRemainingQuantity);
        
        product.setRemainingQuantity(newRemainingQuantity);
        product.setTotalRemainingQuantity(newTotalRemainingQuantity);
    }
    
    private void handleBlockUnblock(Product product, BigDecimal quantityChange, boolean isBlock) {
//        if (isBlock && product.getRemainingQuantity().compareTo(quantityChange) < 0) {
//            throw new ValidationException("Insufficient stock for product: " + product.getName());
//        }
        
        BigDecimal newBlockedQuantity = product.getBlockedQuantity().add(isBlock ? quantityChange : quantityChange.negate());
        BigDecimal newRemainingQuantity = product.getRemainingQuantity().add(isBlock ? quantityChange.negate() : quantityChange);
        
//        if (newBlockedQuantity.compareTo(BigDecimal.ZERO) < 0) {
//            throw new ValidationException("Operation would result in negative blocked quantity for product: " + product.getName());
//        }
        
        validateNonNegativeQuantity(product, newRemainingQuantity, null);
        
        product.setBlockedQuantity(newBlockedQuantity);
        product.setRemainingQuantity(newRemainingQuantity);
    }
    
    private void validateNonNegativeQuantity(Product product, BigDecimal remaining, BigDecimal totalRemaining) {
//        if (remaining != null && remaining.compareTo(BigDecimal.ZERO) < 0) {
//            throw new ValidationException("Operation would result in negative stock for product: " + product.getName());
//        }
//        if (totalRemaining != null && totalRemaining.compareTo(BigDecimal.ZERO) < 0) {
//            throw new ValidationException("Operation would result in negative total stock for product: " + product.getName());
//        }
    }
    
    private void logQuantityUpdate(Product product, BigDecimal quantityChange, Boolean isPurchase, Boolean isSale, Boolean isBlock) {
        log.info("Product {} quantity updated. Change: {}, Purchase: {}, Sale: {}, Block: {}, Remaining: {}, Blocked: {}", 
            product.getId(), quantityChange, isPurchase, isSale, isBlock, 
            product.getRemainingQuantity(), product.getBlockedQuantity());
    }

   
    @Transactional
    public void setProductQuantities(
        Long productId,
        BigDecimal remainingQuantity,
        BigDecimal blockedQuantity,
        BigDecimal totalRemainingQuantity
    ) {
        Lock lock = getProductLock(productId);
        lock.lock();
        try {
            Product product = getAndValidateProduct(productId);
            
            // Store original values for logging
            BigDecimal originalRemaining = product.getRemainingQuantity();
            BigDecimal originalBlocked = product.getBlockedQuantity();
            BigDecimal originalTotal = product.getTotalRemainingQuantity();
            
            // Update quantities if provided
            if (remainingQuantity != null) {
                product.setRemainingQuantity(remainingQuantity);
            }
            
            if (blockedQuantity != null) {
                product.setBlockedQuantity(blockedQuantity);
            }
            
            if (totalRemainingQuantity != null) {
                product.setTotalRemainingQuantity(totalRemainingQuantity);
            } else {
                // Calculate total remaining quantity if not provided
                product.setTotalRemainingQuantity(
                    product.getRemainingQuantity().subtract(product.getBlockedQuantity())
                );
            }
            
            // Validate the new quantities
            validateQuantities(product);
            
            // Save the updated product
            productRepository.save(product);
            
            // Log the quantity changes
            log.info("Product {} quantities updated directly. Changes: " +
                    "Remaining: {} -> {}, " +
                    "Blocked: {} -> {}, " +
                    "Total: {} -> {}",
                product.getId(),
                originalRemaining, product.getRemainingQuantity(),
                originalBlocked, product.getBlockedQuantity(),
                originalTotal, product.getTotalRemainingQuantity()
            );
            
        } catch (Exception e) {
            log.error("Error setting product quantities: {}", e.getMessage(), e);
            throw e;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Validates the product quantities to ensure they are consistent
     */
    private void validateQuantities(Product product) {
        // Ensure quantities are not null
        if (product.getRemainingQuantity() == null) {
            product.setRemainingQuantity(BigDecimal.ZERO);
        }
        if (product.getBlockedQuantity() == null) {
            product.setBlockedQuantity(BigDecimal.ZERO);
        }
        if (product.getTotalRemainingQuantity() == null) {
            product.setTotalRemainingQuantity(BigDecimal.ZERO);
        }
        
        // Validate that blocked quantity is not greater than remaining quantity
        // if (product.getBlockedQuantity().compareTo(product.getRemainingQuantity()) > 0) {
        //     throw new ValidationException(
        //         String.format("Blocked quantity (%s) cannot be greater than remaining quantity (%s)",
        //             product.getBlockedQuantity(), product.getRemainingQuantity())
        //     );
        // }
        
        // Validate that total remaining quantity matches the calculation
        BigDecimal expectedTotal = product.getRemainingQuantity().subtract(product.getBlockedQuantity());
        if (product.getTotalRemainingQuantity().compareTo(expectedTotal) != 0) {
            product.setTotalRemainingQuantity(expectedTotal);
        }
    }
}