package com.inventory.service;

import com.inventory.dao.PurchaseDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.PurchaseItemDto;
import com.inventory.dto.PurchaseRequestDto;
import com.inventory.entity.Customer;
import com.inventory.entity.Product;
import com.inventory.entity.Purchase;
import com.inventory.entity.PurchaseItem;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.CustomerRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.PurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseService {
    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final ProductQuantityService productQuantityService;
    private final CustomerRepository customerRepository;
    private final BatchProcessingService batchProcessingService;
    private final UtilityService utilityService;
    private final PurchaseDao purchaseDao;
    
    @Transactional
    public ApiResponse<?> createPurchase(PurchaseRequestDto request) {
        validatePurchaseRequest(request);
        UserMaster currentUser = utilityService.getCurrentLoggedInUser();
        
        Purchase purchase = new Purchase();
        purchase.setCustomer(customerRepository.findById(request.getCustomerId())
            .orElseThrow(() -> new ValidationException("Customer not found")));
        purchase.setPurchaseDate(request.getPurchaseDate());
        purchase.setInvoiceNumber(request.getInvoiceNumber());
        purchase.setClient(currentUser.getClient());
        purchase.setCreatedBy(currentUser);
        
        // Process items in batches
        List<PurchaseItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (PurchaseItemDto itemDto : request.getProducts()) {
            PurchaseItem item = createPurchaseItem(itemDto, purchase);
            items.add(item);
            totalAmount = totalAmount.add(itemDto.getFinalPrice());
            productQuantityService.updateProductQuantity(
                    item.getProduct().getId(),
                    item.getQuantity()
            );
        }
        
        purchase.setTotalPurchaseAmount(totalAmount);
        purchase = purchaseRepository.save(purchase);
        
        // Process items and update product quantities in batches
//        batchProcessingService.processPurchaseItems(items);
        
        return ApiResponse.success("Purchase created successfully");
    }
    
    private PurchaseItem createPurchaseItem(PurchaseItemDto dto, Purchase purchase) {
        Product product = productRepository.findById(dto.getProductId())
            .orElseThrow(() -> new ValidationException("Product not found"));
            
        PurchaseItem item = new PurchaseItem();
        item.setProduct(product);
        item.setPurchase(purchase);
        item.setQuantity(dto.getQuantity());
        item.setUnitPrice(dto.getUnitPrice());
        item.setDiscountPercentage(dto.getDiscountPercentage());
        item.setDiscountAmount(dto.getDiscountAmount());
        item.setFinalPrice(dto.getFinalPrice());
        item.setRemainingQuantity(dto.getQuantity());
        item.setClient(purchase.getClient());
        
        return item;
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> searchPurchases(Map<String, Object> searchParams) {
        return purchaseDao.searchPurchases(searchParams);
    }
}