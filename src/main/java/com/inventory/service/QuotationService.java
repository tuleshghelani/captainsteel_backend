package com.inventory.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.dao.QuotationDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.QuotationDto;
import com.inventory.dto.QuotationItemCalculationDto;
import com.inventory.dto.QuotationItemRequestDto;
import com.inventory.dto.QuotationRequestDto;
import com.inventory.dto.QuotationStatusUpdateDto;
import com.inventory.entity.Customer;
import com.inventory.entity.Product;
import com.inventory.entity.Quotation;
import com.inventory.entity.QuotationItem;
import com.inventory.entity.QuotationItemCalculation;
import com.inventory.entity.UserMaster;
import com.inventory.enums.PolyCarbonateType;
import com.inventory.enums.ProductMainType;
import com.inventory.enums.QuotationStatus;
import com.inventory.exception.ValidationException;
import com.inventory.repository.CustomerRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.QuotationItemCalculationRepository;
import com.inventory.repository.QuotationItemRepository;
import com.inventory.repository.QuotationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotationService {
    private static final BigDecimal INCHES_IN_FOOT = BigDecimal.valueOf(12);
    private static final BigDecimal SQ_FEET_MULTIPLIER = BigDecimal.valueOf(3.5);
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
    public ApiResponse<?> createQuotation(QuotationRequestDto request) {
        try {
            validateQuotationRequest(request);
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Quotation quotation = new Quotation();
            
            // if(request.getQuotationId() != null){
            //     quotation = quotationRepository.findById(request.getQuotationId())
            //     .orElseThrow(() -> new ValidationException("Quotation not found"));
                
            //     if(quotation.getClient().getId() != currentUser.getClient().getId()){
            //         throw new ValidationException("Unauthorized access to quotation");
            //     }
            // }
            if(request.getCustomerId() != null){
                Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ValidationException("Customer not found"));
                quotation.setCustomer(customer);
                quotation.setCustomerName(customer.getName());
            } else {
                quotation.setCustomerName(request.getCustomerName());
            }

            quotation.setQuoteDate(request.getQuoteDate());
            quotation.setValidUntil(request.getValidUntil());
            quotation.setRemarks(request.getRemarks());
            quotation.setTermsConditions(request.getTermsConditions());
            quotation.setContactNumber(request.getContactNumber());
            quotation.setAddress(request.getAddress());
            quotation.setStatus(QuotationStatus.Q);
            quotation.setClient(currentUser.getClient());
            quotation.setCreatedBy(currentUser);

            // Generate quote number
            String quoteNumber = quoteNumberGeneratorService.generateQuoteNumber(currentUser.getClient());
            System.out.println("Generated quote number: " + quoteNumber);

            // Set the generated quote number
            quotation.setQuoteNumber(quoteNumber);
            
            quotation = quotationRepository.save(quotation);
            
            List<QuotationItem> items = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal taxAmount = BigDecimal.ZERO;
            BigDecimal discountedPrice = BigDecimal.ZERO;
            BigDecimal loadingCharge = BigDecimal.ZERO;

            for (QuotationItemRequestDto itemDto : request.getItems()) {
                QuotationItem item = createQuotationItem(itemDto, quotation, currentUser);
                items.add(item);
                totalAmount = totalAmount.add(item.getFinalPrice());
                taxAmount = taxAmount.add(item.getTaxAmount());
                discountedPrice = discountedPrice.add(item.getDiscountPrice());
                loadingCharge = loadingCharge.add(item.getLoadingCharge());
            }
            
            quotationItemRepository.saveAll(items);

            totalAmount = totalAmount.setScale(0, RoundingMode.HALF_UP);
            quotation.setTotalAmount(totalAmount);
            quotation.setTaxAmount(taxAmount);
            quotation.setDiscountedPrice(discountedPrice);
            quotation.setLoadingCharge(loadingCharge);
            quotationRepository.save(quotation);
            
            return ApiResponse.success("Quotation created successfully");
        } catch (Exception e) {
            e.printStackTrace();
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

            if(quotation.getStatus() == QuotationStatus.A || quotation.getStatus() == QuotationStatus.P ||
                quotation.getStatus() == QuotationStatus.C) {
                throw new ValidationException("Quotation is already accepted, processed or completed");
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
            quotationItemCalculationRepository.deleteByQuotationId(quotation.getId());
            quotationItemRepository.deleteByQuotationId(quotation.getId());
            
            List<QuotationItem> items = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal taxAmount = BigDecimal.ZERO;
            BigDecimal discountedPrice = BigDecimal.ZERO;
            BigDecimal loadingCharge = BigDecimal.ZERO;
            
            for (QuotationItemRequestDto itemDto : request.getItems()) {
                QuotationItem item = createQuotationItem(itemDto, quotation, currentUser);
                items.add(item);
                totalAmount = totalAmount.add(item.getFinalPrice());
                taxAmount = taxAmount.add(item.getTaxAmount());
                discountedPrice = discountedPrice.add(item.getDiscountPrice());
                loadingCharge = loadingCharge.add(item.getLoadingCharge());
            }
            
            quotationItemRepository.saveAll(items);

            totalAmount = totalAmount.setScale(0, RoundingMode.HALF_UP);
            quotation.setTotalAmount(totalAmount);
            quotation.setTaxAmount(taxAmount);
            quotation.setDiscountedPrice(discountedPrice);
            quotation.setLoadingCharge(loadingCharge);
            quotationRepository.save(quotation);
            
            return ApiResponse.success("Quotation updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error updating quotation", e);
            throw new ValidationException("Failed to update quotation: " + e.getMessage());
        }
    }

    private void validateAndProcessItem(QuotationItemRequestDto itemDto, Product product, UserMaster currentUser) {
        if (product.getType() == ProductMainType.REGULAR) {
            validateRegularProductCalculations(itemDto);
            calculateMeasurements(itemDto, product, currentUser);
        } else if (product.getType() == ProductMainType.POLY_CARBONATE) {
            validatePolyCarbonateProduct(product, itemDto);
            calculateMeasurements(itemDto, product, currentUser);
        } else if (product.getType() == ProductMainType.NOS) {
            validateNosProduct(itemDto);
        }
        
        // Set default tax percentage if not provided
        if (itemDto.getTaxPercentage() == null) {
            itemDto.setTaxPercentage(DEFAULT_TAX_PERCENTAGE);
        }
        
        // Set default discount percentage if not provided
        if (itemDto.getDiscountPercentage() == null) {
            itemDto.setDiscountPercentage(BigDecimal.ZERO);
        }
    }

    private void validateRegularProductCalculations(QuotationItemRequestDto itemDto) {
        if (itemDto.getCalculations() == null || itemDto.getCalculations().isEmpty()) {
            throw new ValidationException("Calculations are required for REGULAR products");
        }

        for (QuotationItemCalculationDto calc : itemDto.getCalculations()) {
            if ("SQ_FEET".equalsIgnoreCase(itemDto.getCalculationType())) {
                if ((calc.getFeet() == null || calc.getFeet().compareTo(BigDecimal.ZERO) <= 0) &&
                    (calc.getInch() == null || calc.getInch().compareTo(BigDecimal.ZERO) <= 0)) {
                    throw new ValidationException("Either feet or inch must be greater than 0");
                }
            } else if ("MM".equalsIgnoreCase(itemDto.getCalculationType())) {
                if (calc.getMm() == null || calc.getMm().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ValidationException("MM measurement must be greater than 0");
                }
            } else {
                throw new ValidationException("Invalid calculation type: " + itemDto.getCalculationType());
            }
            
            if (calc.getNos() == null || calc.getNos() <= 0) {
                throw new ValidationException("NOS must be greater than 0");
            }
        }
    }

    private void validatePolyCarbonateProduct(Product product, QuotationItemRequestDto itemDto) {
        if (product.getPolyCarbonateType() == null) {
            throw new ValidationException("Please set poly_carbonate_type in " + product.getName());
        }
        validateRegularProductCalculations(itemDto);
    }

    private void validateNosProduct(QuotationItemRequestDto itemDto) {
        if (itemDto.getQuantity() == null || itemDto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Quantity must be greater than 0 for NOS products");
        }
    }

    private void calculateMeasurements(QuotationItemRequestDto itemDto, Product product, UserMaster currentUser) {
        if ("MM".equalsIgnoreCase(itemDto.getCalculationType())) {
            calculateMMeasurements(itemDto, product, currentUser);
        } else if ("SQ_FEET".equalsIgnoreCase(itemDto.getCalculationType())) {
            calculateSqFeetMeasurements(itemDto, product, currentUser);
        } else {
            throw new ValidationException("Invalid calculation type: " + itemDto.getCalculationType());
        }
    }

    private void calculateSqFeetMeasurements(QuotationItemRequestDto itemDto, Product product, UserMaster currentUser) {
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalSqFeet = BigDecimal.ZERO;
        
        for (QuotationItemCalculationDto calc : itemDto.getCalculations()) {
            if((calc.getFeet() == null || calc.getFeet().compareTo(BigDecimal.ZERO) <= 0) && 
               (calc.getInch() == null || calc.getInch().compareTo(BigDecimal.ZERO) <= 0)) {
                throw new ValidationException("Either feet or inch must be greater than 0");
            }
            if((calc.getNos() == null || Objects.equals(calc.getNos(), 0))) {
                throw new ValidationException("NOS must be greater than 0");
            }

            // Convert feet to inches first
            BigDecimal feetToInches = calc.getFeet().multiply(INCHES_IN_FOOT);
            // Add additional inches
            BigDecimal totalInches = feetToInches.add(calc.getInch());
            
            // Calculate running feet without intermediate rounding
            BigDecimal runningFeet = totalInches
                .multiply(BigDecimal.valueOf(calc.getNos()))
                .divide(INCHES_IN_FOOT, 3, RoundingMode.HALF_UP);  // Use 4 decimal places for intermediate calculation
                
            // Calculate sq feet and weight
//            BigDecimal sqFeet = runningFeet.multiply(SQ_FEET_MULTIPLIER)
//                .setScale(2, RoundingMode.HALF_UP);  // Final rounding to 2 decimal places
            BigDecimal sqFeet = BigDecimal.ZERO;

            if(Objects.equals(product.getType(), ProductMainType.REGULAR)) {
                sqFeet = runningFeet.multiply(SQ_FEET_MULTIPLIER)
                        .setScale(2, RoundingMode.HALF_UP);
            } else if (Objects.equals(product.getType(), ProductMainType.POLY_CARBONATE)) {
                BigDecimal multiplier = getPolyCarbonateMultiplier(product.getPolyCarbonateType());
                sqFeet = runningFeet.multiply(multiplier).setScale(3, RoundingMode.HALF_UP);
            }
            BigDecimal weight = calculateWeight(runningFeet, product);
            
            // Update calculation object
            calc.setRunningFeet(runningFeet.setScale(3, RoundingMode.HALF_UP));
            calc.setSqFeet(sqFeet);

            if(Objects.equals(product.getType(), ProductMainType.REGULAR)) {
                calc.setWeight(weight);

                // Accumulate totals
                totalWeight = totalWeight.add(weight);
            }
            totalSqFeet = totalSqFeet.add(sqFeet);

            // Calculate meter from sq feet instead of running feet
            BigDecimal meter = sqFeet.divide(SQ_FEET_TO_METER, 4, RoundingMode.HALF_UP);
            calc.setMeter(meter);
        }
        
        // Update item totals
        if(Objects.equals(product.getType(), ProductMainType.REGULAR)) {
            itemDto.setWeight(totalWeight);
            itemDto.setQuantity(totalWeight);
            itemDto.setLoadingCharge(totalWeight.multiply(BigDecimal.valueOf(0.1)).setScale(2, RoundingMode.HALF_UP));
        } else if (Objects.equals(product.getType(), ProductMainType.POLY_CARBONATE)) {
            itemDto.setQuantity(totalSqFeet);
            itemDto.setWeight(BigDecimal.ZERO);
        }
    }

    private void calculateMMeasurements(QuotationItemRequestDto itemDto, Product product, UserMaster currentUser) {
//        if (product.getType() != ProductMainType.REGULAR ||
//            !"MM".equalsIgnoreCase(itemDto.getCalculationType())) {
//            throw new ValidationException("MM calculations are only valid for REGULAR products with MM calculation type");
//        }

        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalSqFeet = BigDecimal.ZERO;

        for (QuotationItemCalculationDto calc : itemDto.getCalculations()) {
            // Validate inputs
            if (calc.getMm() == null || calc.getMm().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("MM measurement must be greater than 0");
            }
            if (calc.getNos() == null || calc.getNos() <= 0) {
                throw new ValidationException("NOS must be greater than 0");
            }

            // Calculate running feet: (MM * NOS) / 304.8
            BigDecimal runningFeet = calc.getMm()
                .multiply(BigDecimal.valueOf(calc.getNos()))
                .divide(MM_TO_FEET_CONVERSION, 4, RoundingMode.HALF_UP);

            // Calculate sq feet
            BigDecimal sqFeet = BigDecimal.ZERO;

            if(Objects.equals(product.getType(), ProductMainType.REGULAR)) {
                sqFeet = runningFeet.multiply(SQ_FEET_MULTIPLIER)
                        .setScale(2, RoundingMode.HALF_UP);
            } else if (Objects.equals(product.getType(), ProductMainType.POLY_CARBONATE)) {
                BigDecimal multiplier = getPolyCarbonateMultiplier(product.getPolyCarbonateType());
                sqFeet = runningFeet.multiply(multiplier).setScale(3, RoundingMode.HALF_UP);
            }

            // Calculate weight
            BigDecimal weight = calculateWeight(runningFeet, product);

            // Update calculation object
            calc.setRunningFeet(runningFeet.setScale(3, RoundingMode.HALF_UP));
            calc.setSqFeet(sqFeet);


            if(Objects.equals(product.getType(), ProductMainType.REGULAR)) {
                calc.setWeight(weight);

                // Accumulate totals
                totalWeight = totalWeight.add(weight);
            }
            totalSqFeet = totalSqFeet.add(sqFeet);

            // Calculate meter
            BigDecimal meter = calc.getMm().divide(MM_TO_METER, 4, RoundingMode.HALF_UP);
            calc.setMeter(meter);
        }

        // Update item totals
        if(Objects.equals(product.getType(), ProductMainType.REGULAR)) {
            itemDto.setWeight(totalWeight);
            itemDto.setQuantity(totalWeight);
            itemDto.setLoadingCharge(totalWeight.multiply(BigDecimal.valueOf(0.1)).setScale(2, RoundingMode.HALF_UP));
        } else if (Objects.equals(product.getType(), ProductMainType.POLY_CARBONATE)) {
            itemDto.setQuantity(totalSqFeet);
            itemDto.setWeight(BigDecimal.ZERO);
        }
    }

    private QuotationItem createQuotationItem(QuotationItemRequestDto itemDto, Quotation quotation, UserMaster currentUser) {
        Product product = productRepository.findById(itemDto.getProductId())
                .orElseThrow(() -> new ValidationException("Product not found"));

        // Validate and process item based on product type
        validateAndProcessItem(itemDto, product, currentUser);

        QuotationItem item = new QuotationItem();
        item.setQuotation(quotation);
        item.setProduct(product);
        item.setQuantity(itemDto.getQuantity());
        item.setWeight(itemDto.getWeight());
        item.setUnitPrice(itemDto.getUnitPrice());
        item.setDiscountPercentage(itemDto.getDiscountPercentage());
        item.setTaxPercentage(itemDto.getTaxPercentage());
        item.setCalculationType(itemDto.getCalculationType());
        item.setLoadingCharge(itemDto.getLoadingCharge());

        // Calculate price components
        BigDecimal subTotal = itemDto.getUnitPrice().multiply(itemDto.getQuantity())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal discountAmount = calculatePercentageAmount(subTotal, itemDto.getDiscountPercentage());
        BigDecimal afterDiscount = subTotal.subtract(discountAmount);
        BigDecimal taxAmount = calculatePercentageAmount(afterDiscount, itemDto.getTaxPercentage());

        item.setDiscountAmount(discountAmount);
        item.setDiscountPrice(afterDiscount);
        item.setTaxAmount(taxAmount);
        
        // Add loading charge to final price for REGULAR type products
        if (product.getType() == ProductMainType.REGULAR) {
            BigDecimal loadingCharge = item.getLoadingCharge() != null ? item.getLoadingCharge() : BigDecimal.ZERO;
            item.setFinalPrice(afterDiscount.add(taxAmount).add(loadingCharge));
        } else {
            item.setFinalPrice(afterDiscount.add(taxAmount));
        }
        item.setClient(currentUser.getClient());

        // Save the item first
        item = quotationItemRepository.save(item);

        // Save calculations if present
        if ((product.getType() == ProductMainType.REGULAR || product.getType() == ProductMainType.POLY_CARBONATE) && itemDto.getCalculations() != null) {
            List<QuotationItemCalculation> quotationItemCalculations = saveCalculations(item, itemDto.getCalculations(), currentUser, quotation);
            System.out.printf("quotationItemCalculations : " + quotationItemCalculations);
        }

        return item;
    }

    private List<QuotationItemCalculation> saveCalculations(QuotationItem item, List<QuotationItemCalculationDto> calculations, UserMaster currentUser, Quotation quotation) {
        List<QuotationItemCalculation> itemCalculations = calculations.stream()
                .map(calc -> {
                    QuotationItemCalculation calcEntity = mapToCalculationEntity(calc, item, currentUser, quotation);
                    return quotationItemCalculationRepository.save(calcEntity);
                })
                .collect(Collectors.toList());

        return itemCalculations;
    }

    private QuotationItemCalculation mapToCalculationEntity(QuotationItemCalculationDto dto, QuotationItem item, 
        UserMaster currentUser, Quotation quotation) {
        QuotationItemCalculation calc = new QuotationItemCalculation();
        calc.setQuotationItem(item);
        calc.setFeet(dto.getFeet());
        calc.setInch(dto.getInch());
        calc.setMm(dto.getMm());
        calc.setNos(dto.getNos());
        calc.setRunningFeet(dto.getRunningFeet());
        calc.setSqFeet(dto.getSqFeet());
        calc.setWeight(dto.getWeight());
        calc.setClient(currentUser.getClient());
        calc.setQuotation(quotation);
        return calc;
    }

    private BigDecimal calculatePercentageAmount(BigDecimal base, BigDecimal percentage) {
        return percentage != null ? 
            base.multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP) : 
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
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Valid quantity is required");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Valid unit price is required");
            }
        });
    }

    private void updateQuotationDetails(Quotation quotation, QuotationRequestDto request, UserMaster currentUser) {
        // quotation.setQuoteDate(request.getQuoteDate());
//        quotation.setQuoteNumber(request.getQuoteNumber());
        quotation.setValidUntil(request.getValidUntil());
        quotation.setRemarks(request.getRemarks());
        quotation.setTermsConditions(request.getTermsConditions());
        quotation.setContactNumber(request.getContactNumber());
        quotation.setAddress(request.getAddress());
        quotation.setUpdatedAt(OffsetDateTime.now());
        quotation.setUpdatedBy(currentUser);
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

    public ApiResponse getQuotationDetail(QuotationDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            request.setClientId(currentUser.getClient().getId());
            
            // Get quotation and verify access
            Quotation quotation = quotationRepository.findById(request.getId())
                .orElseThrow(() -> new ValidationException("Quotation not found"));
                
            if (!quotation.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized access to quotation");
            }
            
            // Get all quotation items with calculations
            List<QuotationItem> items = quotationItemRepository.findByQuotationId(quotation.getId());
            List<QuotationItemCalculation> calculations = quotationItemCalculationRepository
                .findByQuotationId(quotation.getId());
                
            // Transform data into response format
            Map<String, Object> response = new HashMap<>();
            
            // Add quotation details
            response.put("id", quotation.getId());
            response.put("quoteNumber", quotation.getQuoteNumber());
            response.put("quoteDate", quotation.getQuoteDate());
            response.put("validUntil", quotation.getValidUntil());
            response.put("totalAmount", quotation.getTotalAmount());
            response.put("status", quotation.getStatus());
            response.put("remarks", quotation.getRemarks());
            response.put("termsConditions", quotation.getTermsConditions());
            response.put("customerName", quotation.getCustomerName());
            response.put("customerId", quotation.getCustomer() != null ? quotation.getCustomer().getId() : null);
            response.put("contactNumber", quotation.getContactNumber());
            response.put("address", quotation.getAddress());
            
            // Transform and add items
            List<Map<String, Object>> itemsList = new ArrayList<>();
            for (QuotationItem item : items) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("productId", item.getProduct().getId());
                itemMap.put("productName", item.getProduct().getName());
                itemMap.put("productType", item.getProduct().getType());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("weight", item.getWeight());
                itemMap.put("unitPrice", item.getUnitPrice());
                itemMap.put("discountPercentage", item.getDiscountPercentage());
                itemMap.put("discountAmount", item.getDiscountAmount());
                itemMap.put("price", item.getDiscountPrice());
                itemMap.put("taxPercentage", item.getTaxPercentage());
                itemMap.put("taxAmount", item.getTaxAmount());
                itemMap.put("finalPrice", item.getFinalPrice());
                itemMap.put("calculationType", item.getCalculationType());
                itemMap.put("loadingCharge", item.getLoadingCharge());
                
                // Add calculations for this item
                List<Map<String, Object>> itemCalculations = calculations.stream()
                    .filter(calc -> calc.getQuotationItem().getId().equals(item.getId()))
                    .map(calc -> {
                        Map<String, Object> calcMap = new HashMap<>();
                        calcMap.put("id", calc.getId());
                        calcMap.put("feet", calc.getFeet());
                        calcMap.put("inch", calc.getInch());
                        calcMap.put("mm", calc.getMm());
                        calcMap.put("nos", calc.getNos());
                        calcMap.put("runningFeet", calc.getRunningFeet());
                        calcMap.put("sqFeet", calc.getSqFeet());
                        calcMap.put("weight", calc.getWeight());
                        return calcMap;
                    })
                    .collect(Collectors.toList());
                    
                itemMap.put("calculations", itemCalculations);
                itemsList.add(itemMap);
            }
            
            response.put("items", itemsList);
            
            return ApiResponse.success("Data fetched successfully", response);
        } catch (Exception e) {
            log.error("Error fetching quotation detail", e);
            throw new ValidationException("Failed to fetch quotation detail: " + e.getMessage());
        }
    }

    public byte[] generateQuotationPdf(QuotationDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            request.setClientId(currentUser.getClient().getId());
            Map<String, Object> quotationData = quotationDao.getQuotationDetail(request);
            return pdfGenerationService.generateQuotationPdf(quotationData);
        } catch (ValidationException ve) {
            ve.printStackTrace();
            throw ve;
        } catch (Exception e) {
            log.error("Error generating quotation PDF", e);
            throw new ValidationException("Failed to generate PDF: " + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> updateQuotationStatus(QuotationStatusUpdateDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Quotation quotation = quotationRepository.findById(request.getId())
                .orElseThrow(() -> new ValidationException("Quotation not found"));

            if (!quotation.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized access to quotation");
            }

            QuotationStatus newStatus = QuotationStatus.valueOf(request.getStatus());
            QuotationStatus currentStatus = quotation.getStatus();

            validateStatusTransition(currentStatus, newStatus);
            handleProductQuantities(quotation, currentStatus, newStatus);

            quotation.setStatus(newStatus);
            quotation.setUpdatedAt(OffsetDateTime.now());
            quotation.setUpdatedBy(currentUser);
            quotationRepository.save(quotation);

            return ApiResponse.success("Quotation status updated successfully");
        } catch (Exception e) {
            log.error("Error updating quotation status", e);
            throw new ValidationException("Failed to update quotation status: " + e.getMessage());
        }
    }

    private void validateStatusTransition(QuotationStatus currentStatus, QuotationStatus newStatus) {
        switch (currentStatus) {
            case Q:
                if (!Arrays.asList(QuotationStatus.A, QuotationStatus.D).contains(newStatus)) {
                    throw new ValidationException("Quote can only be Accepted or Declined");
                }
                break;
            case A:
                if (!Arrays.asList(QuotationStatus.D, QuotationStatus.P).contains(newStatus)) {
                    throw new ValidationException("Accepted quote can only be changed to Processing or Declined");
                }
                break;
            case P:
                if (newStatus != QuotationStatus.C) {
                    throw new ValidationException("Processing quote can only be Completed");
                }
                break;
            case D:
                if (newStatus != QuotationStatus.A) {
                    throw new ValidationException("Declined quote can only be changed to Accepted");
                }
                break;
            case C:
                throw new ValidationException("Current status cannot be updated");
            default:
                throw new ValidationException("Invalid current status");
        }
    }

    private void handleProductQuantities(Quotation quotation, QuotationStatus currentStatus, QuotationStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        if (currentStatus == QuotationStatus.Q && newStatus == QuotationStatus.A) {
            // Block quantities when accepting
            updateProductQuantities(quotation, true);
        } else if (currentStatus == QuotationStatus.A && newStatus == QuotationStatus.D) {
            // Unblock quantities when declining
            updateProductQuantities(quotation, false);
        } else if (currentStatus == QuotationStatus.P && newStatus == QuotationStatus.C) {
            // Move quantities from blocked to used (subtract from blocked)
            updateProductQuantities(quotation, false);
        }
    }

    private void updateProductQuantities(Quotation quotation, boolean block) {
        List<QuotationItem> items = quotationItemRepository.findByQuotationId(quotation.getId());
        
        for (QuotationItem item : items) {
            Product product = item.getProduct();
            productQuantityService.updateProductQuantity(
                product.getId(), 
                item.getQuantity(),
                false,  // not a purchase
                false,  // not a sale
                block   // block or unblock based on status
            );
        }
    }

    private BigDecimal calculateWeight(BigDecimal runningFeet, Product product) {
        // BigDecimal baseWeight = runningFeet.multiply(product.getWeight());
        
        if (product.getType() == ProductMainType.POLY_CARBONATE) {
            BigDecimal multiplier = getPolyCarbonateMultiplier(product.getPolyCarbonateType());
            return runningFeet.multiply(multiplier).setScale(3, RoundingMode.HALF_UP);
        } else if (product.getType() == ProductMainType.REGULAR) {
            return runningFeet.multiply(product.getWeight())
                    .setScale(3, RoundingMode.HALF_UP);
        } else {
            throw new ValidationException("Invalid product type");
        }
        
        // return baseWeight.setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal getPolyCarbonateMultiplier(PolyCarbonateType type) {
        return switch (type) {
            case SINGLE -> SINGLE_MULTIPLIER;
            case DOUBLE -> DOUBLE_MULTIPLIER;
            case FULL_SHEET -> FULL_SHEET_MULTIPLIER;
            default -> throw new ValidationException("Invalid poly_carbonate_type");
        };
    }

    public byte[] generateDispatchSlipPdf(QuotationDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            request.setClientId(currentUser.getClient().getId());
            Map<String, Object> quotationData = quotationDao.getQuotationDetail(request);
            return dispatchSlipPdfService.generateDispatchSlipPdf(quotationData);
        } catch (ValidationException ve) {
            log.error("Error generating dispatch slip PDF", ve);
            throw ve;
        } catch (Exception e) {
            log.error("Error generating dispatch slip PDF", e);
            throw new ValidationException("Failed to generate dispatch slip PDF: " + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> deleteQuotation(QuotationRequestDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Quotation quotation = quotationRepository.findById(request.getQuotationId())
                .orElseThrow(() -> new ValidationException("Quotation not found", HttpStatus.UNPROCESSABLE_ENTITY));

            if (!quotation.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to delete this quotation", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            // Validate status
            if (!Arrays.asList(QuotationStatus.Q, QuotationStatus.D).contains(quotation.getStatus())) {
                throw new ValidationException("Only quotations with status 'Quote' or 'Declined' can be deleted", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            // If quotation is in Accepted status and has blocked quantities, unblock them
//            if (quotation.getStatus() == QuotationStatus.A || quotation.getStatus() == QuotationStatus.P ||
//                 quotation.getStatus() == QuotationStatus.C) {
//                List<QuotationItem> items = quotationItemRepository.findByQuotationId(quotation.getId());
//                for (QuotationItem item : items) {
//                    productQuantityService.updateProductQuantity(
//                        item.getProduct().getId(),
//                        item.getQuantity(),
//                        false,
//                        false,
//                        null
//                    );
//                }
//            }

            quotationItemCalculationRepository.deleteByQuotationId(quotation.getId());
            quotationItemRepository.deleteByQuotationId(quotation.getId());
            quotationRepository.delete(quotation);

            return ApiResponse.success("Quotation deleted successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting quotation", e);
            throw new ValidationException("Failed to delete quotation: " + e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
} 