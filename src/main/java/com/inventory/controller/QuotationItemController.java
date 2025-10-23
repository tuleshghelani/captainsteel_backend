package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.QuotationDto;
import com.inventory.dto.QuotationItemRequestDto;
import com.inventory.service.QuotationItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/quotation-items")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
@Slf4j
public class QuotationItemController {

    private final QuotationItemService quotationItemService;

    @PutMapping("/status")
    public ResponseEntity<ApiResponse<?>> updateStatus(@RequestBody QuotationItemRequestDto request) {
        log.debug("Update quotation item status for ID: {}", request.getId());
        return ResponseEntity.ok(quotationItemService.updateQuotationItemStatus(request));
    }

    @PutMapping("/production")
    public ResponseEntity<ApiResponse<?>> updateProduction(@RequestBody QuotationItemRequestDto request) {
        log.debug("Update quotation item production for ID: {}", request.getId());
        return ResponseEntity.ok(quotationItemService.updateQuotationItemProduction(request));
    }
    
    @PostMapping("/search-with-details")
    public ResponseEntity<?> searchQuotationItemsWithDetails(@RequestBody QuotationDto searchParams) {
        log.debug("Received search quotation items with details request: {}", searchParams);
        Map<String, Object> result = quotationItemService.searchQuotationItemsWithDetails(searchParams);
        return ResponseEntity.ok(result);
    }
}