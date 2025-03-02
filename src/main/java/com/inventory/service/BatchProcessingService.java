package com.inventory.service;

import com.inventory.entity.Purchase;
import com.inventory.entity.PurchaseItem;
import com.inventory.entity.SaleItem;
import com.inventory.dao.PurchaseDao;
import com.inventory.entity.UserMaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchProcessingService {
    private final ProductQuantityService productQuantityService;
    private final PurchaseDao purchaseDao;
    private final UtilityService utilityService;
    private static final int BATCH_SIZE = 100;
    
    @Transactional
    public void processInventoryItems(List<?> items, boolean isPurchase) {
        // Process items in batches of 100
        for (int i = 0; i < items.size(); i += BATCH_SIZE) {
            List<?> batch = items.subList(
                i, 
                Math.min(i + BATCH_SIZE, items.size())
            );
            
            CompletableFuture.runAsync(() -> {
                batch.forEach(item -> {
                    try {
                        if (item instanceof PurchaseItem purchaseItem) {
                            productQuantityService.updateProductQuantity(
                                purchaseItem.getProduct().getId(),
                                purchaseItem.getQuantity(),
                                isPurchase,  // true for purchase, false for sale
                                false,
                                null
                            );
                        } else if (item instanceof SaleItem saleItem) {
                            productQuantityService.updateProductQuantity(
                                saleItem.getProduct().getId(),
                                saleItem.getQuantity(),
                                isPurchase,  // true for purchase, false for sale
                                false,
                                null
                            );
                        }
                    } catch (Exception e) {
                        String itemType = isPurchase ? "purchase" : "sale";
                        log.error("Error processing {} item: {}", itemType, e.getMessage(), e);
                        throw new RuntimeException("Failed to process " + itemType + " item", e);
                    }
                });
            }).exceptionally(throwable -> {
                log.error("Batch processing failed: {}", throwable.getMessage(), throwable);
                return null;
            });
        }
    }
    
    // For backward compatibility
    @Transactional
    public void processPurchaseItems(List<PurchaseItem> items) {
        processInventoryItems(items, true);
    }
    
    // New method for sale items
    @Transactional
    public void processSaleItems(List<SaleItem> items) {
        processInventoryItems(items, false);
    }
    
    @Transactional(readOnly = true)
    public List<Purchase> findPurchasesInBatches(LocalDateTime startDate, LocalDateTime endDate) {
        UserMaster currentUser = utilityService.getCurrentLoggedInUser();
        List<Purchase> allPurchases = new ArrayList<>();
        int offset = 0;
        
        while (true) {
            List<Purchase> batch = purchaseDao.findPurchasesByDateRange(
                startDate, endDate, BATCH_SIZE, offset, currentUser.getClient().getId()
            );
            
            if (batch.isEmpty()) {
                break;
            }
            
            allPurchases.addAll(batch);
            offset += BATCH_SIZE;
        }
        
        return allPurchases;
    }
}