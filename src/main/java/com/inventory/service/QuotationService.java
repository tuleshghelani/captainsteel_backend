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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.dao.QuotationDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.QuotationDto;
import com.inventory.dto.QuotationItemCalculationDto;
import com.inventory.dto.QuotationItemRequestDto;
import com.inventory.dto.QuotationRequestDto;
import com.inventory.dto.QuotationStatusUpdateDto;
import com.inventory.dto.QuotationChartRequestDto;
import com.inventory.dto.QuotationStatusChartResponseDto;
import com.inventory.dto.QuotationTrendChartResponseDto;
import com.inventory.dto.QuotationCustomerChartResponseDto;
import com.inventory.dto.QuotationProductChartResponseDto;
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
    private final QuotationWithOutPdfGenerationService quotationWithOutPdfGenerationService;
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
            Customer customer = null;
            if(request.getCustomerId() != null){
                customer = customerRepository.findById(request.getCustomerId())
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
            
            // Set contact number: use from request if provided, otherwise use customer's mobile if customer exists
            String contactNumber = request.getContactNumber();
            if ((contactNumber == null || contactNumber.trim().isEmpty()) && customer != null && customer.getMobile() != null) {
                contactNumber = customer.getMobile();
            }
            quotation.setContactNumber(contactNumber);
            quotation.setAddress(request.getAddress());
            quotation.setStatus(QuotationStatus.Q);
            quotation.setClient(currentUser.getClient());
            quotation.setCreatedBy(currentUser);
            quotation.setQuotationDiscount(request.getQuotationDiscount() != null ? request.getQuotationDiscount() : BigDecimal.ZERO);
            quotation.setQuotationDiscountAmount(request.getQuotationDiscountAmount() != null ? request.getQuotationDiscountAmount() : BigDecimal.ZERO);

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

            // Apply quotation discount to tax amount
            BigDecimal quotationDiscountAmount = calculateQuotationDiscount(taxAmount, quotation.getQuotationDiscount(), quotation.getQuotationDiscountAmount());
            taxAmount = taxAmount.subtract(quotationDiscountAmount);
            
            totalAmount = totalAmount.setScale(0, RoundingMode.HALF_UP);
            quotation.setTotalAmount(totalAmount);
            quotation.setTaxAmount(taxAmount);
            quotation.setDiscountedPrice(discountedPrice);
            quotation.setLoadingCharge(loadingCharge);
            quotation.setQuotationDiscountAmount(quotationDiscountAmount); // Set the calculated discount amount
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

            if(quotation.getStatus() == QuotationStatus.C) {
                throw new ValidationException("Quotation is already completed");
            }
            
            // Store the original items if the quotation is in 'A' or 'P' status
            List<QuotationItem> originalItems = new ArrayList<>();
            if (quotation.getStatus() == QuotationStatus.A || quotation.getStatus() == QuotationStatus.P) {
                originalItems = quotationItemRepository.findByQuotationId(quotation.getId());
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
            
            // If the quotation was in 'A' or 'P' status, release the quantities of original items
            if (quotation.getStatus() == QuotationStatus.A || quotation.getStatus() == QuotationStatus.P) {
                // Release quantities: add to remaining, subtract from blocked
                for (QuotationItem item : originalItems) {
                    Product product = item.getProduct();
                    productQuantityService.updateProductQuantity(
                        product.getId(), 
                        item.getQuantity(),
                        false,  // not a purchase
                        false,  // not a sale
                        false   // unblock (release)
                    );
                }
            }
            
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

                if (quotation.getStatus() == QuotationStatus.A || quotation.getStatus() == QuotationStatus.P) {
                    // Block quantities: subtract from remaining, add to blocked
                    Product product = item.getProduct();
                    productQuantityService.updateProductQuantity(
                            product.getId(),
                            item.getQuantity(),
                            false,  // not a purchase
                            false,  // not a sale
                            true    // block
                    );
                }
            }
            
            quotationItemRepository.saveAll(items);

            // Apply quotation discount to tax amount
            BigDecimal quotationDiscountAmount = calculateQuotationDiscount(taxAmount, quotation.getQuotationDiscount(), quotation.getQuotationDiscountAmount());
            taxAmount = taxAmount.subtract(quotationDiscountAmount);

            totalAmount = totalAmount.setScale(0, RoundingMode.HALF_UP);
            quotation.setTotalAmount(totalAmount);
            quotation.setTaxAmount(taxAmount);
            quotation.setDiscountedPrice(discountedPrice);
            quotation.setLoadingCharge(loadingCharge);
            quotation.setQuotationDiscountAmount(quotationDiscountAmount); // Set the calculated discount amount
            quotationRepository.save(quotation);

            // If the quotation is now in 'A' or 'P' status, block the quantities of new items
//            if (quotation.getStatus() == QuotationStatus.A || quotation.getStatus() == QuotationStatus.P) {
//                // Block quantities: subtract from remaining, add to blocked
//                for (QuotationItem item : items) {
//                    Product product = item.getProduct();
//                    productQuantityService.updateProductQuantity(
//                        product.getId(),
//                        item.getQuantity(),
//                        false,  // not a purchase
//                        false,  // not a sale
//                        true    // block
//                    );
//                }
//            }
            
            return ApiResponse.success("Quotation updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error updating quotation", e);
            throw new ValidationException("Failed to update quotation: " + e.getMessage());
        }
    }

    private void validateAndProcessItem(QuotationItemRequestDto itemDto, Product product, UserMaster currentUser) {
        // Validate calculationBase='N' can only be used when there's exactly 1 calculation
        if ("N".equalsIgnoreCase(itemDto.getCalculationBase())) {
            if (itemDto.getCalculations() == null || itemDto.getCalculations().size() != 1) {
                throw new ValidationException("CalculationBase 'N' (NOS) can only be selected when there is exactly 1 row in calculations");
            }
        }
        
        if (product.getType() == ProductMainType.REGULAR) {
            // Check if calculation type is NOS - if so, just validate quantity
            if ("NOS".equalsIgnoreCase(itemDto.getCalculationType())) {
                validateNosProduct(itemDto);
            } else {
                validateRegularProductCalculations(itemDto);
                calculateMeasurements(itemDto, product, currentUser);
            }
        } else if (product.getType() == ProductMainType.POLY_CARBONATE) {
            // Check if calculation type is NOS - if so, just validate quantity
            if ("NOS".equalsIgnoreCase(itemDto.getCalculationType())) {
                validateNosProduct(itemDto);
            } else {
                validatePolyCarbonateProduct(product, itemDto);
                calculateMeasurements(itemDto, product, currentUser);
            }
        } else if (product.getType() == ProductMainType.POLY_CARBONATE_ROLL) {
            validatePolyCarbonateRollProduct(product, itemDto);
            calculatePolyCarbonateRollMeasurements(itemDto, product, currentUser);
        } else if (product.getType() == ProductMainType.NOS) {
            validateNosProduct(itemDto);
        } else if (product.getType() == ProductMainType.ACCESSORIES) {
            validateAccessoriesProduct(product, itemDto);
            calculateAccessoriesQuantity(itemDto, product);
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

    private void validateAccessoriesProduct(Product product, QuotationItemRequestDto itemDto) {
        if (itemDto.getAccessoriesSize() == null || itemDto.getAccessoriesSize().trim().isEmpty()) {
            throw new ValidationException("accessoriesSize is required for ACCESSORIES products");
        }
        if (itemDto.getNos() == null || itemDto.getNos() <= 0) {
            throw new ValidationException("Nos must be greater than 0 for ACCESSORIES products");
        }
        
        // For Custom size ("C"), we don't need to check if it exists in the product's accessoriesWeight map
        if (!"C".equals(itemDto.getAccessoriesSize())) {
            if (product.getAccessoriesWeight() == null || product.getAccessoriesWeight().isEmpty()) {
                throw new ValidationException("Accessories weights are not configured for product: " + product.getName());
            }
            if (!product.getAccessoriesWeight().containsKey(itemDto.getAccessoriesSize())) {
                throw new ValidationException("Invalid accessories size: " + itemDto.getAccessoriesSize());
            }
        } else {
            // For Custom size, weight is required
            if (itemDto.getWeight() == null || itemDto.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Weight is required for Custom accessories size");
            }
        }
    }

    private void calculateAccessoriesQuantity(QuotationItemRequestDto itemDto, Product product) {
        BigDecimal total;
        if ("C".equals(itemDto.getAccessoriesSize())) {
            // For Custom size, use the provided weight
            total = itemDto.getWeight().multiply(BigDecimal.valueOf(itemDto.getNos()))
                    .setScale(3, RoundingMode.HALF_UP);
            itemDto.setQuantity(total);
            // Weight is already set by the user for Custom size
            // No loading charge for Custom size ('C')
            itemDto.setLoadingCharge(BigDecimal.ZERO);
        } else {
            BigDecimal unitWeight = product.getAccessoriesWeight().get(itemDto.getAccessoriesSize());
            total = unitWeight.multiply(BigDecimal.valueOf(itemDto.getNos()))
                    .setScale(3, RoundingMode.HALF_UP);
            itemDto.setQuantity(total);
            itemDto.setWeight(total);
            // Calculate loading charge for ACCESSORIES products when accessoriesSize != 'C'
            itemDto.setLoadingCharge(total.multiply(BigDecimal.valueOf(0.1)).setScale(2, RoundingMode.HALF_UP));
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

    private void validatePolyCarbonateRollProduct(Product product, QuotationItemRequestDto itemDto) {
        // Default calculationBase to 'SF' for POLY_CARBONATE_ROLL if null or empty
        String calculationBase = itemDto.getCalculationBase();
        if (calculationBase == null || calculationBase.trim().isEmpty()) {
            calculationBase = "SF";
            itemDto.setCalculationBase("SF");
        } else {
            calculationBase = calculationBase.trim();
            itemDto.setCalculationBase(calculationBase);
        }
        
        // If calculationBase is 'M' (Manual), validate manual quantity
        if ("M".equalsIgnoreCase(calculationBase)) {
            if (itemDto.getQuantity() == null || itemDto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Quantity must be greater than 0 when calculationBase is 'M' for POLY_CARBONATE_ROLL products");
            }
            // For Manual mode, calculations are not required
            return;
        }
        
        // For 'SF' (Square Feet) mode, validate calculations with length and width
        if (itemDto.getCalculations() == null || itemDto.getCalculations().isEmpty()) {
            throw new ValidationException("Calculations with length and width are required for POLY_CARBONATE_ROLL products when calculationBase is 'SF'");
        }
    
        for (QuotationItemCalculationDto calc : itemDto.getCalculations()) {
            if (calc.getLength() == null || calc.getLength().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Length must be greater than 0 for POLY_CARBONATE_ROLL products");
            }
            if (calc.getWidth() == null || calc.getWidth().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Width must be greater than 0 for POLY_CARBONATE_ROLL products");
            }
        }
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

    private void calculatePolyCarbonateRollMeasurements(QuotationItemRequestDto itemDto, Product product, UserMaster currentUser) {
        // Default calculationBase to 'SF' if null or empty
        String calculationBase = itemDto.getCalculationBase();
        if (calculationBase == null || calculationBase.trim().isEmpty()) {
            calculationBase = "SF";
            itemDto.setCalculationBase("SF");
        } else {
            calculationBase = calculationBase.trim();
            itemDto.setCalculationBase(calculationBase);
        }
        
        // 'SF' (Square Feet) mode: calculate from length * width
        BigDecimal totalQuantity = BigDecimal.ZERO;
        
        if (itemDto.getCalculations() == null || itemDto.getCalculations().isEmpty()) {
            throw new ValidationException("Calculations are required for POLY_CARBONATE_ROLL products when calculationBase is 'SF'");
        }
        
        for (QuotationItemCalculationDto calc : itemDto.getCalculations()) {
            if (calc.getLength() == null || calc.getLength().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Length must be greater than 0");
            }
            if (calc.getWidth() == null || calc.getWidth().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Width must be greater than 0");
            }
    
            // Calculate total: length * width
            BigDecimal total = calc.getLength().multiply(calc.getWidth())
                    .setScale(4, RoundingMode.HALF_UP);
            
            // Store total in sqFeet field for consistency (can be used for display)
            calc.setSqFeet(total);
            
            // Accumulate totals
            totalQuantity = totalQuantity.add(total);
        }

        itemDto.setWeight(BigDecimal.ZERO);
        // No loading charge for POLY_CARBONATE_ROLL
        itemDto.setLoadingCharge(BigDecimal.ZERO);
        // Handle based on calculationBase
        if ("M".equalsIgnoreCase(calculationBase)) {
            // Manual mode: use manual quantity directly
            if (itemDto.getQuantity() == null || itemDto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Quantity must be greater than 0 when calculationBase is 'M'");
            }
            itemDto.setQuantity(itemDto.getQuantity().setScale(3, RoundingMode.HALF_UP));
            return;
        }
        
        // Set the total quantity as the item quantity
        itemDto.setQuantity(totalQuantity.setScale(3, RoundingMode.HALF_UP));
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
                // Use product's sq_feet_multiplier, default to 3.5 if null or zero
                BigDecimal multiplier = DEFAULT_SQ_FEET_MULTIPLIER;
                if (product.getSqFeetMultiplier() != null && product.getSqFeetMultiplier().compareTo(BigDecimal.ZERO) > 0) {
                    multiplier = product.getSqFeetMultiplier();
                }
                sqFeet = runningFeet.multiply(multiplier)
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
        
        // Update item totals based on calculationBase
        if(Objects.equals(product.getType(), ProductMainType.REGULAR)) {
            // Default to 'W' (Weight) if calculationBase is null or empty
            String calculationBase = itemDto.getCalculationBase();
            if (calculationBase == null || calculationBase.trim().isEmpty()) {
                calculationBase = "W"; // Default to Weight
            }
            
            switch (calculationBase) {
                case "RF": // Running Feet
                    BigDecimal totalRunningFeet = BigDecimal.ZERO;
                    for (QuotationItemCalculationDto calc : itemDto.getCalculations()) {
                        totalRunningFeet = totalRunningFeet.add(calc.getRunningFeet());
                    }
                    itemDto.setWeight(totalRunningFeet);
                    itemDto.setQuantity(totalRunningFeet);
                    // No loading charge for RF
                    itemDto.setLoadingCharge(BigDecimal.ZERO);
                    break;
                case "SF": // Sq. Feet
                    itemDto.setWeight(totalSqFeet);
                    itemDto.setQuantity(totalSqFeet);
                    // No loading charge for SF
                    itemDto.setLoadingCharge(BigDecimal.ZERO);
                    break;
                case "N": // NOS - total of Nos
                    Long totalNos = 0L;
                    for (QuotationItemCalculationDto calc : itemDto.getCalculations()) {
                        if (calc.getNos() != null) {
                            totalNos += calc.getNos();
                        }
                    }
                    itemDto.setWeight(BigDecimal.ZERO);
                    itemDto.setQuantity(BigDecimal.valueOf(totalNos));
                    // No loading charge for N
                    itemDto.setLoadingCharge(BigDecimal.ZERO);
                    break;
                case "W": // Weight (default)
                default:
                    itemDto.setWeight(totalWeight);
                    itemDto.setQuantity(totalWeight);
                    // Calculate loading charge only when calculationBase is 'W' or null
                    itemDto.setLoadingCharge(itemDto.getQuantity().multiply(BigDecimal.valueOf(0.1)).setScale(2, RoundingMode.HALF_UP));
                    break;
            }
        } else if (Objects.equals(product.getType(), ProductMainType.POLY_CARBONATE)) {
            // Check if calculationBase is null or empty - if so, calculate loading charge
            String calculationBase = itemDto.getCalculationBase();
            boolean isCalculationBaseNull = (calculationBase == null || calculationBase.trim().isEmpty());
            
            // Ensure calculationBase is not null for switch statement
            if (isCalculationBaseNull) {
                calculationBase = "SF"; // Default to Sq. Feet for POLY_CARBONATE for quantity calculation
            } else {
                calculationBase = calculationBase != null ? calculationBase.trim() : "SF";
            }
            
            // Ensure calculationBase is not null before switch
            String finalCalculationBase = calculationBase != null ? calculationBase : "SF";
            switch (finalCalculationBase) {
                case "N": // NOS - total of Nos
                    Long totalNos = 0L;
                    for (QuotationItemCalculationDto calc : itemDto.getCalculations()) {
                        if (calc.getNos() != null) {
                            totalNos += calc.getNos();
                        }
                    }
                    itemDto.setQuantity(BigDecimal.valueOf(totalNos));
                    itemDto.setWeight(BigDecimal.ZERO);
                    // No loading charge for N
                    itemDto.setLoadingCharge(BigDecimal.ZERO);
                    break;
                case "W": // Weight - calculate loading charge
                    itemDto.setQuantity(totalSqFeet);
                    itemDto.setWeight(BigDecimal.ZERO);
                    // Calculate loading charge when calculationBase is 'W'
                    itemDto.setLoadingCharge(itemDto.getQuantity().multiply(BigDecimal.valueOf(0.1)).setScale(2, RoundingMode.HALF_UP));
                    break;
                case "SF": // Sq. Feet (default)
                default:
                    itemDto.setQuantity(totalSqFeet);
                    itemDto.setWeight(BigDecimal.ZERO);
                    // Calculate loading charge when calculationBase is null, otherwise no loading charge for SF
                    if (isCalculationBaseNull) {
                        itemDto.setLoadingCharge(itemDto.getQuantity().multiply(BigDecimal.valueOf(0.1)).setScale(2, RoundingMode.HALF_UP));
                    } else {
                        itemDto.setLoadingCharge(BigDecimal.ZERO);
                    }
                    break;
            }
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
                // Use product's sq_feet_multiplier, default to 3.5 if null or zero
                BigDecimal multiplier = DEFAULT_SQ_FEET_MULTIPLIER;
                if (product.getSqFeetMultiplier() != null && product.getSqFeetMultiplier().compareTo(BigDecimal.ZERO) > 0) {
                    multiplier = product.getSqFeetMultiplier();
                }
                sqFeet = runningFeet.multiply(multiplier)
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

        // Update item totals based on calculationBase
        if(Objects.equals(product.getType(), ProductMainType.REGULAR)) {
            // Default to 'W' (Weight) if calculationBase is null or empty
            String calculationBase = itemDto.getCalculationBase();
            if (calculationBase == null || calculationBase.trim().isEmpty()) {
                calculationBase = "W"; // Default to Weight
            }
            
            switch (calculationBase) {
                case "RF": // Running Feet
                    BigDecimal totalRunningFeet = BigDecimal.ZERO;
                    for (QuotationItemCalculationDto calc : itemDto.getCalculations()) {
                        totalRunningFeet = totalRunningFeet.add(calc.getRunningFeet());
                    }
                    itemDto.setWeight(totalRunningFeet);
                    itemDto.setQuantity(totalRunningFeet);
                    // No loading charge for RF
                    itemDto.setLoadingCharge(BigDecimal.ZERO);
                    break;
                case "SF": // Sq. Feet
                    itemDto.setWeight(totalSqFeet);
                    itemDto.setQuantity(totalSqFeet);
                    // No loading charge for SF
                    itemDto.setLoadingCharge(BigDecimal.ZERO);
                    break;
                case "N": // NOS - total of Nos
                    Long totalNos = 0L;
                    for (QuotationItemCalculationDto calc : itemDto.getCalculations()) {
                        if (calc.getNos() != null) {
                            totalNos += calc.getNos();
                        }
                    }
                    itemDto.setWeight(BigDecimal.ZERO);
                    itemDto.setQuantity(BigDecimal.valueOf(totalNos));
                    // No loading charge for N
                    itemDto.setLoadingCharge(BigDecimal.ZERO);
                    break;
                case "W": // Weight (default)
                default:
                    itemDto.setWeight(totalWeight);
                    itemDto.setQuantity(totalWeight);
                    // Calculate loading charge only when calculationBase is 'W' or null
                    itemDto.setLoadingCharge(itemDto.getQuantity().multiply(BigDecimal.valueOf(0.1)).setScale(2, RoundingMode.HALF_UP));
                    break;
            }
        } else if (Objects.equals(product.getType(), ProductMainType.POLY_CARBONATE)) {
            // Check if calculationBase is null or empty - if so, calculate loading charge
            String calculationBase = itemDto.getCalculationBase();
            boolean isCalculationBaseNull = (calculationBase == null || calculationBase.trim().isEmpty());
            
            // Ensure calculationBase is not null for switch statement
            if (isCalculationBaseNull) {
                calculationBase = "SF"; // Default to Sq. Feet for POLY_CARBONATE for quantity calculation
            } else {
                calculationBase = calculationBase != null ? calculationBase.trim() : "SF";
            }
            
            // Ensure calculationBase is not null before switch
            String finalCalculationBase = calculationBase != null ? calculationBase : "SF";
            switch (finalCalculationBase) {
                case "N": // NOS - total of Nos
                    Long totalNos = 0L;
                    for (QuotationItemCalculationDto calc : itemDto.getCalculations()) {
                        if (calc.getNos() != null) {
                            totalNos += calc.getNos();
                        }
                    }
                    itemDto.setQuantity(BigDecimal.valueOf(totalNos));
                    itemDto.setWeight(BigDecimal.ZERO);
                    // No loading charge for N
                    itemDto.setLoadingCharge(BigDecimal.ZERO);
                    break;
                case "W": // Weight - calculate loading charge
                    itemDto.setQuantity(totalSqFeet);
                    itemDto.setWeight(BigDecimal.ZERO);
                    // Calculate loading charge when calculationBase is 'W'
                    itemDto.setLoadingCharge(itemDto.getQuantity().multiply(BigDecimal.valueOf(0.1)).setScale(2, RoundingMode.HALF_UP));
                    break;
                case "SF": // Sq. Feet (default)
                default:
                    itemDto.setQuantity(totalSqFeet);
                    itemDto.setWeight(BigDecimal.ZERO);
                    // Calculate loading charge when calculationBase is null, otherwise no loading charge for SF
                    if (isCalculationBaseNull) {
                        itemDto.setLoadingCharge(itemDto.getQuantity().multiply(BigDecimal.valueOf(0.1)).setScale(2, RoundingMode.HALF_UP));
                    } else {
                        itemDto.setLoadingCharge(BigDecimal.ZERO);
                    }
                    break;
            }
        }
    }

    private QuotationItem createQuotationItem(QuotationItemRequestDto itemDto, Quotation quotation, UserMaster currentUser) {
        Product product = productRepository.findById(itemDto.getProductId())
                .orElseThrow(() -> new ValidationException("Product not found"));

        if(!Objects.equals(product.getClient().getId(), currentUser.getClient().getId())) {
            throw new ValidationException("Product is not available for you");
        }
        // Validate and process item based on product type
        validateAndProcessItem(itemDto, product, currentUser);

        QuotationItem item = new QuotationItem();
        item.setQuotation(quotation);
        item.setProduct(product);
        item.setQuantity(itemDto.getQuantity());
        item.setWeight(itemDto.getWeight());
        if (product.getType() == ProductMainType.ACCESSORIES) {
            item.setAccessoriesSize(itemDto.getAccessoriesSize());
            item.setNos(itemDto.getNos()); // Set nos for ACCESSORIES
            // For Custom size, use the provided weight; otherwise, get from product's accessoriesWeight map
            if ("C".equals(itemDto.getAccessoriesSize())) {
                item.setAccessoriesWeight(itemDto.getWeight());
            } else {
                BigDecimal unitWeight = product.getAccessoriesWeight().get(itemDto.getAccessoriesSize());
                item.setAccessoriesWeight(unitWeight);
            }
        }
        item.setUnitPrice(itemDto.getUnitPrice());
        item.setDiscountPercentage(itemDto.getDiscountPercentage());
        item.setTaxPercentage(itemDto.getTaxPercentage());
        item.setCalculationType(itemDto.getCalculationType());
        item.setCalculationBase(itemDto.getCalculationBase()); // Set calculationBase
        item.setLoadingCharge(itemDto.getLoadingCharge());
        if(itemDto.getQuotationItemStatus()!=null){
            item.setQuotationItemStatus(itemDto.getQuotationItemStatus());
        } else if (itemDto.getQuotationItemStatus()==null && itemDto.getIsProduction() != null && itemDto.getIsProduction()) {
            item.setQuotationItemStatus("O");
        }
        // Calculate price components
        BigDecimal subTotal = itemDto.getUnitPrice().multiply(itemDto.getQuantity())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal discountAmount = calculatePercentageAmount(subTotal, itemDto.getDiscountPercentage());
        BigDecimal afterDiscount = subTotal.subtract(discountAmount);
        BigDecimal taxAmount = calculatePercentageAmount(afterDiscount, itemDto.getTaxPercentage());

        // Calculate quotation discount amount for this item based on the quotation's discount
        BigDecimal itemQuotationDiscountAmount = calculateQuotationDiscountForItem(taxAmount, quotation.getQuotationDiscount());
        BigDecimal adjustedTaxAmount = taxAmount.subtract(itemQuotationDiscountAmount);

        item.setDiscountAmount(discountAmount);
        item.setDiscountPrice(afterDiscount);
        item.setTaxAmount(adjustedTaxAmount);
        item.setQuotationDiscountAmount(itemQuotationDiscountAmount);
        
        // Add loading charge to final price for REGULAR, ACCESSORIES, and POLY_CARBONATE type products
        // For REGULAR: when calculationBase='W' or null
        // For ACCESSORIES: when accessoriesSize!='C'
        // For POLY_CARBONATE: when calculationBase='W' or null
        // POLY_CARBONATE_ROLL: no loading charge
        BigDecimal loadingCharge = item.getLoadingCharge() != null ? item.getLoadingCharge() : BigDecimal.ZERO;
        if (product.getType() == ProductMainType.REGULAR || product.getType() == ProductMainType.ACCESSORIES || 
            product.getType() == ProductMainType.POLY_CARBONATE) {
            item.setFinalPrice(afterDiscount.add(adjustedTaxAmount).add(loadingCharge));
        } else {
            item.setFinalPrice(afterDiscount.add(adjustedTaxAmount));
        }
        item.setClient(currentUser.getClient());

        // Set item remarks if provided
        if (itemDto.getItemRemarks() != null) {
            item.setItemRemarks(itemDto.getItemRemarks().trim());
        }
        if(itemDto.getIsProduction() != null){
            item.setIsProduction(itemDto.getIsProduction());
        }

        // Save the item first
        item = quotationItemRepository.save(item);

        // Save calculations if present
        if ((product.getType() == ProductMainType.REGULAR || product.getType() == ProductMainType.POLY_CARBONATE || 
             product.getType() == ProductMainType.POLY_CARBONATE_ROLL) && itemDto.getCalculations() != null) {
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
        calc.setLength(dto.getLength());
        calc.setWidth(dto.getWidth());
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
    
    private BigDecimal calculateQuotationDiscount(BigDecimal taxAmount, BigDecimal quotationDiscount, BigDecimal quotationDiscountAmount) {
        // If quotationDiscountAmount is provided (not zero), use it directly
        if (quotationDiscountAmount != null && quotationDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Ensure the discount amount doesn't exceed the tax amount
            if (quotationDiscountAmount.compareTo(taxAmount) > 0) {
                return taxAmount;
            }
            return quotationDiscountAmount;
        }
        
        // Otherwise, calculate based on percentage
        if (quotationDiscount == null || quotationDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Ensure discount doesn't exceed 100%
        if (quotationDiscount.compareTo(BigDecimal.valueOf(100)) > 0) {
            quotationDiscount = BigDecimal.valueOf(100);
        }
        
        return taxAmount.multiply(quotationDiscount)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate quotation discount amount for an individual item based on the quotation's discount percentage
     * @param itemTaxAmount The tax amount for this specific item
     * @param quotationDiscount The quotation discount percentage
     * @return The discount amount to be applied to this item's tax
     */
    private BigDecimal calculateQuotationDiscountForItem(BigDecimal itemTaxAmount, BigDecimal quotationDiscount) {
        if (itemTaxAmount == null || itemTaxAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        if (quotationDiscount == null || quotationDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Ensure discount doesn't exceed 100%
        if (quotationDiscount.compareTo(BigDecimal.valueOf(100)) > 0) {
            quotationDiscount = BigDecimal.valueOf(100);
        }
        
        return itemTaxAmount.multiply(quotationDiscount)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }



    private void validateQuotationRequest(QuotationRequestDto request) {
        if (request.getQuoteDate() == null) {
            throw new ValidationException("Quote date is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ValidationException("At least one item is required");
        }
        
        // Validate quotation discount range (0 to 100)
        if (request.getQuotationDiscount() != null && 
            (request.getQuotationDiscount().compareTo(BigDecimal.ZERO) < 0 || 
             request.getQuotationDiscount().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new ValidationException("Quotation discount must be between 0 and 100");
        }
        
        request.getItems().forEach(item -> {
            if (item.getProductId() == null) {
                throw new ValidationException("Product ID is required");
            }
            // For ACCESSORIES, quantity is derived; require accessoriesSize and nos instead
            // For POLY_CARBONATE_ROLL, quantity is derived from calculations (length * width)
            Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ValidationException("Product not found"));
            if (product.getType() == ProductMainType.ACCESSORIES) {
                if (item.getAccessoriesSize() == null || item.getAccessoriesSize().trim().isEmpty()) {
                    throw new ValidationException("accessoriesSize is required for ACCESSORIES items");
                }
                if (item.getNos() == null || item.getNos() <= 0) {
                    throw new ValidationException("Nos must be greater than 0 for ACCESSORIES items");
                }
                // For Custom size, weight is required and will be used for quantity calculation
                if ("C".equals(item.getAccessoriesSize())) {
                    if (item.getWeight() == null || item.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new ValidationException("Weight is required for Custom accessories size");
                    }
                }
            } else if (product.getType() == ProductMainType.POLY_CARBONATE_ROLL) {
                // For POLY_CARBONATE_ROLL, check calculationBase
                String calculationBase = item.getCalculationBase();
                if (calculationBase == null || calculationBase.trim().isEmpty()) {
                    calculationBase = "SF"; // Default to SF
                    item.setCalculationBase("SF");
                } else {
                    calculationBase = calculationBase.trim();
                    item.setCalculationBase(calculationBase);
                }
                
                if ("M".equalsIgnoreCase(calculationBase)) {
                    // For Manual mode, validate manual quantity
                    if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new ValidationException("Manual quantity is required and must be greater than 0 for POLY_CARBONATE_ROLL items when calculationBase is 'M'");
                    }
                    // Quantity will be set to manualQuantity in calculation method
                } else {
                    // For SF mode, validate calculations with length and width
                    if (item.getCalculations() == null || item.getCalculations().isEmpty()) {
                        throw new ValidationException("Calculations with length and width are required for POLY_CARBONATE_ROLL items when calculationBase is 'SF'");
                    }
                    // Quantity will be calculated from length * width in calculations
                }
            } else {
                if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ValidationException("Valid quantity is required");
                }
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
        
        // Set contact number: use from request if provided, otherwise use customer's mobile if customer exists
        String contactNumber = request.getContactNumber();
        if ((contactNumber == null || contactNumber.trim().isEmpty()) && quotation.getCustomer() != null && quotation.getCustomer().getMobile() != null) {
            contactNumber = quotation.getCustomer().getMobile();
        }
        quotation.setContactNumber(contactNumber);
        quotation.setAddress(request.getAddress());
        quotation.setUpdatedAt(OffsetDateTime.now());
        quotation.setUpdatedBy(currentUser);
        quotation.setQuotationDiscount(request.getQuotationDiscount() != null ? request.getQuotationDiscount() : BigDecimal.ZERO);
        quotation.setQuotationDiscountAmount(request.getQuotationDiscountAmount() != null ? request.getQuotationDiscountAmount() : BigDecimal.ZERO);
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
            response.put("quotationDiscount", quotation.getQuotationDiscount());
            response.put("quotationDiscountAmount", quotation.getQuotationDiscountAmount());
            
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
                itemMap.put("calculationBase", item.getCalculationBase());
                itemMap.put("loadingCharge", item.getLoadingCharge());
                itemMap.put("accessoriesSize", item.getAccessoriesSize());
                itemMap.put("accessoriesWeight", item.getAccessoriesWeight());
                itemMap.put("nos", item.getNos()); // Add nos field for ACCESSORIES
                itemMap.put("itemRemarks", item.getItemRemarks());
                itemMap.put("quotationItemStatus", item.getQuotationItemStatus());
                itemMap.put("isProduction", item.getIsProduction());
                itemMap.put("quotationDiscountAmount", item.getQuotationDiscountAmount());
                
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
                        calcMap.put("length", calc.getLength());
                        calcMap.put("width", calc.getWidth());
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

    public ResponseEntity<byte[]> generateQuotationPdfWithMetadata(QuotationDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            request.setClientId(currentUser.getClient().getId());
            Map<String, Object> quotationData = quotationDao.getQuotationDetail(request);
            
            // Check if quotation discount is 0 or null, then use PdfGenerationService
            // Otherwise, use QuotationWithOutPdfGenerationService
            BigDecimal quotationDiscount = (BigDecimal) quotationData.get("quotationDiscount");
            byte[] pdfBytes;
            if (quotationDiscount == null || quotationDiscount.compareTo(BigDecimal.ZERO) <= 0) {
                pdfBytes = pdfGenerationService.generateQuotationPdf(quotationData);
            } else {
                pdfBytes = quotationWithOutPdfGenerationService.generateQuotationPdf(quotationData);
            }
            
            // Generate filename from quote number and customer name
            String quoteNumber = (String) quotationData.get("quoteNumber");
            String customerName = (String) quotationData.get("customerName");
            String filenamePart = (quoteNumber != null ? quoteNumber : "quotation") + "_" + 
                                  (customerName != null ? customerName : "");
            String filename = sanitizeFilename(filenamePart + ".pdf");
            
            // Prepare HTTP response with headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", filename);
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (ValidationException ve) {
            ve.printStackTrace();
            throw ve;
        } catch (Exception e) {
            log.error("Error generating quotation PDF", e);
            throw new ValidationException("Failed to generate PDF: " + e.getMessage());
        }
    }
    
    public byte[] generateQuotationPdf(QuotationDto request) {
        ResponseEntity<byte[]> result = generateQuotationPdfWithMetadata(request);
        return result.getBody();
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

        if ((currentStatus == QuotationStatus.Q && newStatus == QuotationStatus.A) ||
            (currentStatus == QuotationStatus.D && newStatus == QuotationStatus.A)) {
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

    public ResponseEntity<byte[]> generateDispatchSlipPdfWithMetadata(QuotationDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            request.setClientId(currentUser.getClient().getId());
            Map<String, Object> quotationData = quotationDao.getQuotationDetail(request);
            byte[] pdfBytes = dispatchSlipPdfService.generateDispatchSlipPdf(quotationData);
            
            // Generate filename from quote number and customer name
            String quoteNumber = (String) quotationData.get("quoteNumber");
            String customerName = (String) quotationData.get("customerName");
            String filenamePart = (quoteNumber != null ? quoteNumber : "quotation") + "_" + 
                                  (customerName != null ? customerName : "") + "_dispatch_slip";
            String filename = sanitizeFilename(filenamePart + ".pdf");
            
            // Prepare HTTP response with headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", filename);
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (ValidationException ve) {
            log.error("Error generating dispatch slip PDF", ve);
            throw ve;
        } catch (Exception e) {
            log.error("Error generating dispatch slip PDF", e);
            throw new ValidationException("Failed to generate dispatch slip PDF: " + e.getMessage());
        }
    }
    
    public byte[] generateDispatchSlipPdf(QuotationDto request) {
        ResponseEntity<byte[]> result = generateDispatchSlipPdfWithMetadata(request);
        return result.getBody();
    }
    
    /**
     * Sanitize filename by removing invalid characters
     * @param filename The filename to sanitize
     * @return Sanitized filename safe for all operating systems
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "quotation.pdf";
        }
        // Replace invalid characters with underscore
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
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
    
    /**
     * Get quotation statistics grouped by status (for pie chart)
     * Returns count and sum of totalAmount for each status
     */
    public ApiResponse<List<QuotationStatusChartResponseDto>> getQuotationStatusChart(QuotationChartRequestDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            request.setClientId(currentUser.getClient().getId());
            
            List<QuotationStatusChartResponseDto> data = quotationDao.getQuotationStatusStatistics(request);
            return ApiResponse.success("Quotation status chart data fetched successfully", data);
        } catch (Exception e) {
            log.error("Error fetching quotation status chart data", e);
            throw new ValidationException("Failed to fetch quotation status chart data: " + e.getMessage());
        }
    }
    
    /**
     * Get quotation trend data grouped by period (month/day/week/year)
     * For line/bar charts showing trends over time
     */
    public ApiResponse<List<QuotationTrendChartResponseDto>> getQuotationTrendChart(QuotationChartRequestDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            request.setClientId(currentUser.getClient().getId());
            
            List<QuotationTrendChartResponseDto> data = quotationDao.getQuotationTrendData(request);
            return ApiResponse.success("Quotation trend chart data fetched successfully", data);
        } catch (Exception e) {
            log.error("Error fetching quotation trend chart data", e);
            throw new ValidationException("Failed to fetch quotation trend chart data: " + e.getMessage());
        }
    }
    
    /**
     * Get top customers by quotation count and total amount
     * For bar charts showing customer performance
     */
    public ApiResponse<List<QuotationCustomerChartResponseDto>> getTopCustomersChart(QuotationChartRequestDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            request.setClientId(currentUser.getClient().getId());
            
            List<QuotationCustomerChartResponseDto> data = quotationDao.getTopCustomersByQuotation(request);
            return ApiResponse.success("Top customers chart data fetched successfully", data);
        } catch (Exception e) {
            log.error("Error fetching top customers chart data", e);
            throw new ValidationException("Failed to fetch top customers chart data: " + e.getMessage());
        }
    }
    
    /**
     * Get top products by quotation count
     * Shows which products appear most frequently in quotations
     */
    public ApiResponse<List<QuotationProductChartResponseDto>> getTopProductsChart(QuotationChartRequestDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            request.setClientId(currentUser.getClient().getId());
            
            List<QuotationProductChartResponseDto> data = quotationDao.getTopProductsByQuotation(request);
            return ApiResponse.success("Top products chart data fetched successfully", data);
        } catch (Exception e) {
            log.error("Error fetching top products chart data", e);
            throw new ValidationException("Failed to fetch top products chart data: " + e.getMessage());
        }
    }
    
    /**
     * Get revenue by status over time (for area/stacked charts)
     * Shows how revenue from different statuses changes over time
     */
    public ApiResponse<List<QuotationTrendChartResponseDto>> getRevenueByStatusOverTimeChart(QuotationChartRequestDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            request.setClientId(currentUser.getClient().getId());
            
            List<QuotationTrendChartResponseDto> data = quotationDao.getRevenueByStatusOverTime(request);
            return ApiResponse.success("Revenue by status over time chart data fetched successfully", data);
        } catch (Exception e) {
            log.error("Error fetching revenue by status over time chart data", e);
            throw new ValidationException("Failed to fetch revenue by status over time chart data: " + e.getMessage());
        }
    }
}