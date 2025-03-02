package com.inventory.service;

import com.inventory.dao.SaleDao;
import com.inventory.dto.*;
import com.inventory.entity.*;
import com.inventory.exception.ValidationException;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final ProductQuantityService productQuantityService;
    private final CustomerRepository customerRepository;
    private final BatchProcessingService batchProcessingService;
    private final UtilityService utilityService;
    private final SaleDao saleDao;
    private final SaleItemRepository saleItemRepository;

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> createSale(SaleRequestDto request) {
        try {
            validateSaleRequest(request);
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            
            // Handle update case
            if (request.getId() != null) {
                return handleSaleUpdate(request, currentUser);
            }
            
            // Existing create logic
            Sale sale = new Sale();
            sale.setCustomer(customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ValidationException("Customer not found")));
            sale.setSaleDate(request.getSaleDate());
            sale.setInvoiceNumber(request.getInvoiceNumber());
            sale.setNumberOfItems(request.getProducts().size());
            sale.setClient(currentUser.getClient());
            sale.setCreatedBy(currentUser);
            sale = saleRepository.save(sale);
            
            // Process items in batches
            List<SaleItem> items = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;

            List<String> coilNumbers = new ArrayList<>();
            
            for (SaleItemDto itemDto : request.getProducts()) {
                SaleItem item = createSaleItem(itemDto, sale);
                items.add(item);
                saleItemRepository.save(item);
                totalAmount = totalAmount.add(item.getFinalPrice());
                if (item.getCoilNumber() != null && !item.getCoilNumber().isEmpty()) {
                    // Escape any special characters including commas
                    String escapedcoilNumber = item.getCoilNumber().replace(",", "\\,");
                    coilNumbers.add(escapedcoilNumber);
                }
    //            productQuantityService.updateProductQuantity(
    //                    item.getProduct().getId(),
    //                    item.getQuantity(),
    //                    true
    //            );
            }
            
            // Round the total amount
            totalAmount = totalAmount.setScale(0, RoundingMode.HALF_UP);
            sale.setTotalSaleAmount(totalAmount);
            sale.setCoilNumbers(coilNumbers);
            sale = saleRepository.save(sale);
            
            // Process items and update product quantities in batches
           batchProcessingService.processSaleItems(items);
            
            return ApiResponse.success("Sale created successfully");
        } catch(ValidationException ve) {
            ve.printStackTrace();
            throw ve;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Sale creation failed: " + e.getMessage());
        }
    }
    
    private SaleItem createSaleItem(SaleItemDto dto, Sale sale) {
        Product product = productRepository.findById(dto.getProductId())
            .orElseThrow(() -> new ValidationException("Product not found"));
            
        SaleItem item = new SaleItem();
        item.setProduct(product);
        item.setSale(sale);
        item.setQuantity(dto.getQuantity());
        item.setRemarks(dto.getRemarks());
        if (dto.getCoilNumber() != null && !dto.getCoilNumber().isEmpty()) {
            item.setCoilNumber(dto.getCoilNumber().toLowerCase());
        }
        item.setUnitPrice(dto.getUnitPrice().setScale(2, RoundingMode.HALF_UP));
//        item.setDiscountPercentage(dto.getDiscountPercentage());
        
        // Calculate amounts with 2 decimal places
        BigDecimal subTotal = dto.getUnitPrice()
            .multiply((dto.getQuantity()))
            .setScale(2, RoundingMode.HALF_UP);
            
        BigDecimal discountAmount = calculateDiscountAmount(subTotal, dto.getDiscountPercentage())
            .setScale(2, RoundingMode.HALF_UP);
        
//        item.setDiscountAmount(discountAmount);
        item.setFinalPrice(subTotal.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP));
//        item.setRemainingQuantity(dto.getQuantity());
        item.setClient(sale.getClient());
        
        return item;
    }

    private BigDecimal calculateDiscountAmount(BigDecimal base, BigDecimal percentage) {
        return percentage != null ? 
            base.multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;
    }
    
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> searchSales(SaleDto searchParams) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            searchParams.setClientId(currentUser.getClient().getId());
            Page<Map<String, Object>> maps = saleDao.searchSales(searchParams);
            return maps;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Purchase retrieval failed: " + e.getMessage());
        }
    }
    
    private void validateSaleRequest(SaleRequestDto request) {
        if (request.getCustomerId() == null) {
            throw new ValidationException("Customer ID is required");
        }
        if (request.getSaleDate() == null) {
            throw new ValidationException("Sale date is required");
        }
//        if (request.getInvoiceNumber() == null || request.getInvoiceNumber().trim().isEmpty()) {
//            throw new ValidationException("Invoice number is required");
//        }
        if (request.getProducts() == null || request.getProducts().isEmpty()) {
            throw new ValidationException("At least one product is required");
        }
        
        request.getProducts().forEach(item -> {
            if (item.getProductId() == null) {
                throw new ValidationException("Product ID is required");
            }
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Valid quantity is required");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Valid unit price is required");
            }
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> delete(Long id) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Sale not found"));
                
            if (!sale.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to delete this sale");
            }
            
            List<SaleItem> items = saleItemRepository.findBySaleId(id);

            for (SaleItem item : items) {
                try {
                    productQuantityService.updateProductQuantity(
                            item.getProduct().getId(),
                            item.getQuantity(),
                            true,  // true to add the quantity,
                            false,
                            null
                    );
                } catch (Exception e) {
                    log.error("Error reversing quantity for product {}: {}",
                            item.getProduct().getId(), e.getMessage());
                    throw new ValidationException("Failed to reverse product quantities: " + e.getMessage());
                }
            }

            saleItemRepository.deleteBySaleId(id);
            saleRepository.delete(sale);
            
            return ApiResponse.success("Sale deleted successfully");
        } catch (ValidationException ve) {
            ve.printStackTrace();
            throw ve;
        } catch (Exception e) {
            log.error("Error deleting sale: {}", e.getMessage(), e);
            throw new ValidationException("Failed to delete sale: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSaleDetail(SaleDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            return saleDao.getSaleDetail(request.getId(), currentUser.getClient().getId());
        } catch (Exception e) {
            log.error("Error fetching sale detail: {}", e.getMessage(), e);
            throw new ValidationException("Failed to fetch sale detail: " + e.getMessage());
        }
    }

    private ApiResponse<?> handleSaleUpdate(SaleRequestDto request, UserMaster currentUser) {
        Sale existingSale = saleRepository.findById(request.getId())
            .orElseThrow(() -> new ValidationException("sale not found"));
            
        if (!existingSale.getClient().getId().equals(currentUser.getClient().getId())) {
            throw new ValidationException("Unauthorized access to sale");
        }
        
        // Reverse existing quantities
        List<SaleItem> existingItems = saleItemRepository.findBySaleId(request.getId());
        for (SaleItem item : existingItems) {
            productQuantityService.updateProductQuantity(
                item.getProduct().getId(),
                item.getQuantity(),
                true,  // not a purchase
                false,   // is a purchase (reverse)
                null    // no blocking
            );
        }
        
        // Delete existing items
        saleItemRepository.deleteBySaleId(request.getId());
        
        // Update sale details
        existingSale.setCustomer(customerRepository.findById(request.getCustomerId())
            .orElseThrow(() -> new ValidationException("Customer not found")));
        existingSale.setSaleDate(request.getSaleDate());
        existingSale.setInvoiceNumber(request.getInvoiceNumber());
        existingSale.setNumberOfItems(request.getProducts().size());
        existingSale.setUpdatedBy(currentUser);
        existingSale.setUpdatedAt(OffsetDateTime.now());
        
        // Process new items
        List<SaleItem> newItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (SaleItemDto itemDto : request.getProducts()) {
            SaleItem item = createSaleItem(itemDto, existingSale);
            newItems.add(item);
            saleItemRepository.save(item);
            totalAmount = totalAmount.add(item.getFinalPrice());
        }
        
        // Round the total amount
        totalAmount = totalAmount.setScale(0, RoundingMode.HALF_UP);
        existingSale.setTotalSaleAmount(totalAmount);
        saleRepository.save(existingSale);
        
        // Process items and update product quantities in batches
        batchProcessingService.processSaleItems(newItems);
        
        return ApiResponse.success("Sale updated successfully");
    }
}