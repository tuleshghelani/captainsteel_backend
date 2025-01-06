package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.PurchaseRequestDto;
import com.inventory.service.PurchaseService;
import com.inventory.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseService purchaseService;
    private final CacheManager cacheManager;
    
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createPurchase(@RequestBody PurchaseRequestDto request) {
        return ResponseEntity.ok(purchaseService.createPurchase(request));
    }
    
//    @GetMapping("/search")
//    @Cacheable(value = "purchaseSearchCache", key = "#searchParams")
//    public ResponseEntity<Map<String, Object>> searchPurchases(
//            @RequestParam(required = false) String invoiceNumber,
//            @RequestParam(required = false) String startDate,
//            @RequestParam(required = false) String endDate,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//
//        Map<String, Object> searchParams = new HashMap<>();
//        searchParams.put("invoiceNumber", invoiceNumber);
//        searchParams.put("startDate", startDate);
//        searchParams.put("endDate", endDate);
//        searchParams.put("page", page);
//        searchParams.put("size", size);
//
//        return ResponseEntity.ok(purchaseService.searchPurchases(searchParams));
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<ApiResponse<?>> delete(@PathVariable Long id) {
//        return ResponseEntity.ok(purchaseService.delete(id));
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse<?>> update(
//            @PathVariable Long id,
//            @RequestBody PurchaseRequestDto request) {
//        return ResponseEntity.ok(purchaseService.update(id, request));
//    }
}