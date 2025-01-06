package com.inventory.controller;

import com.inventory.dto.ProductDto;
import com.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProductDto request) {
        return ResponseEntity.ok(productService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ProductDto request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return ResponseEntity.ok(productService.delete(id));
    }

    @PostMapping("/getProducts")
    public ResponseEntity<?> getProducts(@RequestBody ProductDto productDto) {
        return ResponseEntity.ok(productService.getProducts(productDto));
    }
    
    @PostMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestBody ProductDto productDto) {
        return ResponseEntity.ok(productService.searchProducts(productDto));
    }
}
