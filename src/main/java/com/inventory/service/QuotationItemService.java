package com.inventory.service;

import com.inventory.dao.QuotationDao;
import com.inventory.dto.*;
import com.inventory.entity.*;
import com.inventory.enums.PolyCarbonateType;
import com.inventory.enums.ProductMainType;
import com.inventory.enums.QuotationStatus;
import com.inventory.enums.QuotationStatusItem;
import com.inventory.exception.ValidationException;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotationItemService {
    private static final BigDecimal INCHES_IN_FOOT = BigDecimal.valueOf(12);
    private static final BigDecimal DEFAULT_SQ_FEET_MULTIPLIER = BigDecimal.valueOf(3.5);
    private static final BigDecimal DEFAULT_TAX_PERCENTAGE = BigDecimal.valueOf(18);
    private static final BigDecimal MM_TO_FEET_CONVERSION = BigDecimal.valueOf(304.8);
    
    private static final int WEIGHT_SCALE = 3;
    private static final RoundingMode WEIGHT_ROUNDING = RoundingMode.HALF_UP;
    
    private static final BigDecimal SINGLE_MULTIPLIER = BigDecimal.valueOf(1.16);
    private static final BigDecimal DOUBLE_MULTIPLIER = BigDecimal.valueOf(2.0);
    private static final BigDecimal FULL_SHEET_MULTIPLIER = BigDecimal.valueOf(4.0);
    
    private static final BigDecimal SQ_FEET_TO_METER = BigDecimal.valueOf(10.764);
    private static final BigDecimal MM_TO_METER = BigDecimal.valueOf(1000);
    
    private final QuotationRepository quotationRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UtilityService utilityService;
    private final QuotationDao quotationDao;
    private final QuoteNumberGeneratorService quoteNumberGeneratorService;
    private final PdfGenerationService pdfGenerationService;
    private final ProductQuantityService productQuantityService;
    private final QuotationItemCalculationRepository quotationItemCalculationRepository;
    private final DispatchSlipPdfService dispatchSlipPdfService;


    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> updateQuotationItemProduction(QuotationItemRequestDto request) {
        UserMaster currentUser = utilityService.getCurrentLoggedInUser();
        QuotationItem item = quotationItemRepository.findById(request.getId())
                .orElseThrow(() -> new ValidationException("Quotation item not found"));
        if (!item.getClient().getId().equals(currentUser.getClient().getId())) {
            throw new ValidationException("Unauthorized access to quotation item");
        }
        item.setIsProduction(request.getIsProduction());
        if(request.getIsProduction()) {
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
        String status = request.getQuotationItemStatus();
        if(status.equals(item.getQuotationItemStatus())) {
            throw new ValidationException("Quotation item status is already " + status);
        }
        int updated = quotationItemRepository.updateQuotationItemStatusById(item.getId(), status);
        if (updated == 0) {
            throw new ValidationException("Failed to update quotation item status");
        }
        return ApiResponse.success("Quotation item status updated successfully");
    }
} 