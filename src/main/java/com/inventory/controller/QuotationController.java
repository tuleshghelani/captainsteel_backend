package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.QuotationDto;
import com.inventory.dto.QuotationRequestDto;
import com.inventory.service.QuotationService;
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
} 