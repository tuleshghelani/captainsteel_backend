package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.QuotationDto;
import com.inventory.dto.QuotationRequestDto;
import com.inventory.dto.QuotationStatusUpdateDto;
import com.inventory.dto.QuotationChartRequestDto;
import com.inventory.dto.QuotationStatusChartResponseDto;
import com.inventory.dto.QuotationTrendChartResponseDto;
import com.inventory.dto.QuotationCustomerChartResponseDto;
import com.inventory.dto.QuotationProductChartResponseDto;
import com.inventory.service.QuotationService;
import com.inventory.service.QuoteNumberResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/quotations")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
@Slf4j
public class QuotationController {
    private final QuotationService quotationService;
    private final QuoteNumberResetService quoteNumberResetService;
    
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<?>> createQuotation(@RequestBody QuotationRequestDto request) {
        return ResponseEntity.ok(quotationService.createQuotation(request));
    }
    
    @PutMapping("/update")
    public ResponseEntity<ApiResponse<?>> updateQuotation(
            @RequestBody QuotationRequestDto request) {
        return ResponseEntity.ok(quotationService.updateQuotation(request));
    }
    
    @PostMapping("/search")
    public ResponseEntity<?> searchQuotations(@RequestBody QuotationDto searchParams) {
        log.debug("Received search quotation request: {}", searchParams);
        return ResponseEntity.ok(quotationService.searchQuotations(searchParams));
    }
    
    @PostMapping("/detail")
    public ResponseEntity<?> getQuotationDetail(@RequestBody QuotationDto request) {
        log.debug("Received quotation detail request for ID: {}", request.getId());
        return ResponseEntity.ok(quotationService.getQuotationDetail(request));
    }
    
    @PostMapping("/generate-pdf")
    public ResponseEntity<byte[]> generateQuotationPdf(@RequestBody QuotationDto request) {
        log.debug("Received quotation PDF generation request for ID: {}", request.getId());
        return quotationService.generateQuotationPdfWithMetadata(request);
    }
    
    @PostMapping("/generate-dispatch-slip")
    public ResponseEntity<byte[]> generateDispatchSlipPdf(@RequestBody QuotationDto request) {
        log.debug("Received dispatch slip PDF generation request for ID: {}", request.getId());
        return quotationService.generateDispatchSlipPdfWithMetadata(request);
    }
    
    @PutMapping("/update-status")
    public ResponseEntity<ApiResponse<?>> updateQuotationStatus(@RequestBody QuotationStatusUpdateDto request) {
        log.debug("Received quotation status update request for ID: {}", request.getId());
        return ResponseEntity.ok(quotationService.updateQuotationStatus(request));
    }
    
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<?>> deleteQuotation(@RequestBody QuotationRequestDto request) {
        log.debug("Received quotation delete request for ID: {}", request.getQuotationId());
        return ResponseEntity.ok(quotationService.deleteQuotation(request));
    }
    
    /**
     * Manual endpoint to reset quote numbers (useful for testing or emergency reset)
     * Should be protected with admin-only access in production
     */
    @PostMapping("/reset-quote-numbers")
    public ResponseEntity<ApiResponse<?>> resetQuoteNumbers() {
        log.info("Manual quote number reset triggered");
        try {
            int resetCount = quoteNumberResetService.resetQuoteNumbers();
            return ResponseEntity.ok(new ApiResponse<>(true, 
                "Quote numbers reset successfully for " + resetCount + " clients", null));
        } catch (Exception e) {
            log.error("Error resetting quote numbers: {}", e.getMessage(), e);
            return ResponseEntity.ok(new ApiResponse<>(false, 
                "Failed to reset quote numbers: " + e.getMessage(), null));
        }
    }
    
    /**
     * Get quotation statistics grouped by status (for pie chart)
     * Returns count and sum of totalAmount for each status
     * POST /api/quotations/charts/status
     */
    @PostMapping("/charts/status")
    public ResponseEntity<ApiResponse<?>> getQuotationStatusChart(@RequestBody QuotationChartRequestDto request) {
        log.debug("Received quotation status chart request: startDate={}, endDate={}", 
            request.getStartDate(), request.getEndDate());
        return ResponseEntity.ok(quotationService.getQuotationStatusChart(request));
    }
    
    /**
     * Get quotation trend data grouped by period (month/day/week/year)
     * For line/bar charts showing trends over time
     * POST /api/quotations/charts/trend
     */
    @PostMapping("/charts/trend")
    public ResponseEntity<ApiResponse<?>> getQuotationTrendChart(@RequestBody QuotationChartRequestDto request) {
        log.debug("Received quotation trend chart request: startDate={}, endDate={}, groupBy={}", 
            request.getStartDate(), request.getEndDate(), request.getGroupBy());
        return ResponseEntity.ok(quotationService.getQuotationTrendChart(request));
    }
    
    /**
     * Get top customers by quotation count and total amount
     * For bar charts showing customer performance
     * POST /api/quotations/charts/top-customers
     */
    @PostMapping("/charts/top-customers")
    public ResponseEntity<ApiResponse<?>> getTopCustomersChart(@RequestBody QuotationChartRequestDto request) {
        log.debug("Received top customers chart request: startDate={}, endDate={}, limit={}", 
            request.getStartDate(), request.getEndDate(), request.getLimit());
        return ResponseEntity.ok(quotationService.getTopCustomersChart(request));
    }
    
    /**
     * Get top products by quotation count
     * Shows which products appear most frequently in quotations
     * POST /api/quotations/charts/top-products
     */
    @PostMapping("/charts/top-products")
    public ResponseEntity<ApiResponse<?>> getTopProductsChart(@RequestBody QuotationChartRequestDto request) {
        log.debug("Received top products chart request: startDate={}, endDate={}, limit={}", 
            request.getStartDate(), request.getEndDate(), request.getLimit());
        return ResponseEntity.ok(quotationService.getTopProductsChart(request));
    }
    
    /**
     * Get revenue by status over time (for area/stacked charts)
     * Shows how revenue from different statuses changes over time
     * POST /api/quotations/charts/revenue-by-status
     */
    @PostMapping("/charts/revenue-by-status")
    public ResponseEntity<ApiResponse<?>> getRevenueByStatusOverTimeChart(@RequestBody QuotationChartRequestDto request) {
        log.debug("Received revenue by status over time chart request: startDate={}, endDate={}, groupBy={}", 
            request.getStartDate(), request.getEndDate(), request.getGroupBy());
        return ResponseEntity.ok(quotationService.getRevenueByStatusOverTimeChart(request));
    }
} 