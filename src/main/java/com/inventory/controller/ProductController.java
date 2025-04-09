package com.inventory.controller;

import com.inventory.dto.ProductDto;
import com.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @PostMapping("/export-pdf")
    public ResponseEntity<?> exportPdf(@RequestBody ProductDto productDto) {
        try {
            byte[] pdfBytes = productService.exportProductsPdf(productDto);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "products.pdf");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(e.getMessage());
        }
    }
}
