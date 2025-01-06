package com.inventory.controller;

import com.inventory.dto.CategoryDto;
import com.inventory.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CategoryDto request) {
        return ResponseEntity.ok(categoryService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CategoryDto request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.delete(id));
    }

    @PostMapping("/getCategories")
    public ResponseEntity<?> getCategories(@RequestBody CategoryDto categoryDto) {
        return ResponseEntity.ok(categoryService.getCategories(categoryDto));
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchCategories(@RequestBody CategoryDto categoryDto) {
        return ResponseEntity.ok(categoryService.searchCategories(categoryDto));
    }
}
