package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.request.BatchDeleteRequestDto;
import com.inventory.service.BatchDeletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/batch")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class BatchDeleteController {
    private final BatchDeletionService batchDeletionService;
    
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<?>> batchDelete(@RequestBody BatchDeleteRequestDto request) {
        return ResponseEntity.ok(batchDeletionService.deleteRecords(request));
    }
} 