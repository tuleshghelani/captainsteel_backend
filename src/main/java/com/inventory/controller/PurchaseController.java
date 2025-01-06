package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.PurchaseRequestDto;
import com.inventory.service.PurchaseService;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import com.inventory.dto.PurchaseDto;

@RestController
@RequestMapping("/api/purchases")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseService purchaseService;
    
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<?>> createPurchase(@RequestBody PurchaseRequestDto request) {
        return ResponseEntity.ok(purchaseService.createPurchase(request));
    }
    
    @PostMapping("/searchPurchase")
    public ResponseEntity<?> searchPurchases(
            @RequestParam(required = false) String invoiceNumber,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PurchaseDto searchParams = new PurchaseDto();
        searchParams.setInvoiceNumber(invoiceNumber);
        searchParams.setCurrentPage(page);
        searchParams.setPerPageRecord(size);
        // Set other search parameters as needed
        
        return ResponseEntity.ok(purchaseService.searchPurchases(searchParams));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.delete(id));
    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse<?>> update(
//            @PathVariable Long id,
//            @RequestBody PurchaseRequestDto request) {
//        return ResponseEntity.ok(purchaseService.update(id, request));
//    }
}