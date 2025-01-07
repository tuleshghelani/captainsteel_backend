package com.inventory.service;

import com.inventory.dao.QuotationDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.QuotationDto;
import com.inventory.dto.QuotationItemRequestDto;
import com.inventory.dto.QuotationRequestDto;
import com.inventory.entity.*;
import com.inventory.enums.QuotationStatus;
import com.inventory.exception.ValidationException;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotationService {
    private final QuotationRepository quotationRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UtilityService utilityService;
    private final QuotationDao quotationDao;
    private final QuoteNumberGeneratorService quoteNumberGeneratorService;

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> createQuotation(QuotationRequestDto request) {
        try {
            validateQuotationRequest(request);
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Quotation quotation = new Quotation();
            
            if(request.getQuotationId() != null){
                quotation = quotationRepository.findById(request.getQuotationId())
                .orElseThrow(() -> new ValidationException("Quotation not found"));
                
                if(quotation.getClient().getId() != currentUser.getClient().getId()){
                    throw new ValidationException("Unauthorized access to quotation");
                }
            }
            if(request.getCustomerId() != null){
                Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ValidationException("Customer not found"));
                quotation.setCustomer(customer);
                quotation.setCustomerName(customer.getName());
            } else {
                quotation.setCustomerName(request.getCustomerName());
            }

            quotation.setQuoteDate(request.getQuoteDate());
            quotation.setQuoteNumber(request.getQuoteNumber());
            quotation.setValidUntil(request.getValidUntil());
            quotation.setRemarks(request.getRemarks());
            quotation.setTermsConditions(request.getTermsConditions());
            quotation.setStatus(QuotationStatus.QUOTE);
            quotation.setClient(currentUser.getClient());
            quotation.setCreatedBy(currentUser);

            // Generate quote number
            String quoteNumber = quoteNumberGeneratorService.generateQuoteNumber(currentUser.getClient());

            // Set the generated quote number
            request.setQuoteNumber(quoteNumber);
            
            quotation = quotationRepository.save(quotation);
            
            List<QuotationItem> items = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            
            for (QuotationItemRequestDto itemDto : request.getItems()) {
                QuotationItem item = createQuotationItem(itemDto, quotation, currentUser);
                items.add(item);
                totalAmount = totalAmount.add(item.getFinalPrice());
            }
            
            quotationItemRepository.saveAll(items);
            
            quotation.setTotalAmount(totalAmount);
            quotationRepository.save(quotation);
            
            return ApiResponse.success("Quotation created successfully");
        } catch (Exception e) {
            log.error("Error creating quotation", e);
            throw new ValidationException("Failed to create quotation: " + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> updateQuotation(QuotationRequestDto request) {
        try {
            validateQuotationRequest(request);
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            
            Quotation quotation = quotationRepository.findById(request.getQuotationId())
                .orElseThrow(() -> new ValidationException("Quotation not found"));
                
            if (!quotation.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized access to quotation");
            }
            
            if(request.getCustomerId() != null){
                Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ValidationException("Customer not found"));
                quotation.setCustomer(customer);
                quotation.setCustomerName(customer.getName());
            } else {
                quotation.setCustomerName(request.getCustomerName());
            }
            
            updateQuotationDetails(quotation, request, currentUser);
            
            // Delete existing items
            quotationItemRepository.deleteByQuotationId(quotation.getId());
            
            List<QuotationItem> items = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            
            for (QuotationItemRequestDto itemDto : request.getItems()) {
                QuotationItem item = createQuotationItem(itemDto, quotation, currentUser);
                items.add(item);
                totalAmount = totalAmount.add(item.getFinalPrice());
            }
            
            quotationItemRepository.saveAll(items);
            quotation.setTotalAmount(totalAmount);
            quotationRepository.save(quotation);
            
            return ApiResponse.success("Quotation updated successfully");
        } catch (Exception e) {
            log.error("Error updating quotation", e);
            throw new ValidationException("Failed to update quotation: " + e.getMessage());
        }
    }

    private QuotationItem createQuotationItem(QuotationItemRequestDto itemDto, Quotation quotation, UserMaster currentUser) {
        Product product = productRepository.findById(itemDto.getProductId())
            .orElseThrow(() -> new ValidationException("Product not found"));
            
        QuotationItem item = new QuotationItem();
        item.setQuotation(quotation);
        item.setProduct(product);
        item.setQuantity(itemDto.getQuantity());
        item.setUnitPrice(itemDto.getUnitPrice().setScale(2, BigDecimal.ROUND_HALF_UP));
        item.setDiscountPercentage(itemDto.getDiscountPercentage());
        item.setTaxPercentage(itemDto.getTaxPercentage());
        
        // Calculate amounts with 2 decimal places
        BigDecimal subTotal = itemDto.getUnitPrice()
            .multiply(BigDecimal.valueOf(itemDto.getQuantity()))
            .setScale(2, BigDecimal.ROUND_HALF_UP);
            
        BigDecimal discountAmount = calculatePercentageAmount(subTotal, itemDto.getDiscountPercentage())
            .setScale(2, BigDecimal.ROUND_HALF_UP);
            
        BigDecimal afterDiscount = subTotal.subtract(discountAmount)
            .setScale(2, BigDecimal.ROUND_HALF_UP);
            
        BigDecimal taxAmount = calculatePercentageAmount(afterDiscount, itemDto.getTaxPercentage())
            .setScale(2, BigDecimal.ROUND_HALF_UP);
            
        BigDecimal discountPrice = afterDiscount;
        
        item.setDiscountPrice(discountPrice);
        item.setDiscountAmount(discountAmount);
        item.setTaxAmount(taxAmount);
        item.setFinalPrice(afterDiscount.add(taxAmount).setScale(2, BigDecimal.ROUND_HALF_UP));
        item.setClient(currentUser.getClient());
        
        return item;
    }

    private BigDecimal calculatePercentageAmount(BigDecimal base, BigDecimal percentage) {
        return percentage != null ? 
            base.multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP) : 
            BigDecimal.ZERO;
    }

    private void validateQuotationRequest(QuotationRequestDto request) {
        if (request.getCustomerName() == null) {
            throw new ValidationException("Customer Name is required");
        }
        if (request.getQuoteDate() == null) {
            throw new ValidationException("Quote date is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ValidationException("At least one item is required");
        }
        
        request.getItems().forEach(item -> {
            if (item.getProductId() == null) {
                throw new ValidationException("Product ID is required");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ValidationException("Valid quantity is required");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Valid unit price is required");
            }
        });
    }

    private void updateQuotationDetails(Quotation quotation, QuotationRequestDto request, UserMaster currentUser) {
        quotation.setQuoteDate(request.getQuoteDate());
        quotation.setQuoteNumber(request.getQuoteNumber());
        quotation.setValidUntil(request.getValidUntil());
        quotation.setRemarks(request.getRemarks());
        quotation.setTermsConditions(request.getTermsConditions());
        quotation.setUpdatedAt(OffsetDateTime.now());
    }

    public Map<String, Object> searchQuotations(QuotationDto searchParams) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            searchParams.setClientId(currentUser.getClient().getId());
            return quotationDao.searchQuotations(searchParams);
        } catch (Exception e) {
            log.error("Error searching quotations", e);
            throw new ValidationException("Failed to search quotations: " + e.getMessage());
        }
    }
} 