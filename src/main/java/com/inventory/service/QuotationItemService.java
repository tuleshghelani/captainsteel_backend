package com.inventory.service;

import com.inventory.dao.QuotationDao;
import com.inventory.dto.*;
import com.inventory.entity.*;
import com.inventory.enums.QuotationStatusItem;
import com.inventory.exception.ValidationException;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuotationItemService {
    private static final Logger logger = LoggerFactory.getLogger(QuotationItemService.class);
    
    private final QuotationItemRepository quotationItemRepository;
    private final UtilityService utilityService;
    private final QuotationDao quotationDao;

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> updateQuotationItemProduction(QuotationItemRequestDto request) {
        UserMaster currentUser = utilityService.getCurrentLoggedInUser();
        QuotationItem item = quotationItemRepository.findById(request.getId())
                .orElseThrow(() -> new ValidationException("Quotation item not found"));
        if (!item.getClient().getId().equals(currentUser.getClient().getId())) {
            throw new ValidationException("Unauthorized access to quotation item");
        }
        item.setIsProduction(request.getIsProduction());
        if(request.getIsProduction() != null && request.getIsProduction()) {
            item.setQuotationItemStatus(QuotationStatusItem.O.value);
        } else {
            item.setQuotationItemStatus(null);
        }
        quotationItemRepository.save(item);
        return ApiResponse.success("Quotation item production flag updated successfully");
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> updateQuotationItemStatus(QuotationItemRequestDto request) {
        UserMaster currentUser = utilityService.getCurrentLoggedInUser();
        QuotationItem item = quotationItemRepository.findById(request.getId())
                .orElseThrow(() -> new ValidationException("Quotation item not found"));
        if (!item.getClient().getId().equals(currentUser.getClient().getId())) {
            throw new ValidationException("Unauthorized access to quotation item");
        }
        item.setQuotationItemStatus(request.getQuotationItemStatus());
        return ApiResponse.success("Quotation item status updated successfully");
    }
    
    /**
     * Search quotation items with quotation details
     * @param searchParams The search parameters
     * @return Map containing the search results and pagination info
     */
    public Map<String, Object> searchQuotationItemsWithDetails(QuotationDto searchParams) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            
            // Set client ID for security
            searchParams.setClientId(currentUser.getClient().getId());
            
            // Handle pagination defaults
            if (searchParams.getCurrentPage() == null) {
                searchParams.setCurrentPage(0);
            }
            if (searchParams.getPerPageRecord() == null) {
                searchParams.setPerPageRecord(10);
            }
            
            // Handle sorting defaults
            if (searchParams.getSortBy() == null) {
                searchParams.setSortBy("id");
            }
            if (searchParams.getSortDir() == null) {
                searchParams.setSortDir("desc");
            }
            
            // Use the DAO method to search quotation items with details
            return quotationDao.searchQuotationItemsWithDetails(searchParams);
        } catch (Exception e) {
            logger.error("Error searching quotation items with details", e);
            throw new ValidationException("Failed to search quotation items: " + e.getMessage());
        }
    }
}