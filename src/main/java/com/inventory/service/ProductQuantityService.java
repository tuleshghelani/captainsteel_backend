package com.inventory.service;

import com.inventory.entity.Product;
import com.inventory.exception.ValidationException;
import com.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class ProductQuantityService {
    private final ProductRepository productRepository;
    private final ConcurrentHashMap<Long, Lock> productLocks = new ConcurrentHashMap<>();
    
    private Lock getProductLock(Long productId) {
        return productLocks.computeIfAbsent(productId, k -> new ReentrantLock());
    }
    
    @Transactional
    public void updateProductQuantity(Long productId, Long quantityChange) {
        Lock lock = getProductLock(productId);
        lock.lock();
        try {
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ValidationException("Product not found"));
            
            Long newRemainingQuantity = product.getRemainingQuantity() + quantityChange;
            Long newTotalRemainingQuantity = product.getTotalRemainingQuantity() + quantityChange;
            
            product.setRemainingQuantity(newRemainingQuantity);
            product.setTotalRemainingQuantity(newTotalRemainingQuantity);
            
            productRepository.save(product);
        } finally {
            lock.unlock();
        }
    }
}