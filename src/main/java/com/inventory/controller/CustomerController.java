package com.inventory.controller;

import com.inventory.dto.CoatingPriceDto;
import com.inventory.dto.CustomerDto;
import com.inventory.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CustomerDto request) {
        return ResponseEntity.ok(customerService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CustomerDto request) {
        return ResponseEntity.ok(customerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.delete(id));
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchCustomers(@RequestBody CustomerDto request) {
        return ResponseEntity.ok(customerService.searchCustomers(request));
    }

    @PostMapping("/getCustomers")
    public ResponseEntity<?> getCustomers(@RequestBody CustomerDto request) {
        return ResponseEntity.ok(customerService.getCustomers(request));
    }

    @PostMapping("/getCustomer")
    public ResponseEntity<?> getCustomer(@RequestBody CustomerDto request) {
        return ResponseEntity.ok(customerService.getCustomer(request.getId()));
    }

    @PostMapping("/coating-price")
    public ResponseEntity<?> getCoatingPrice(@RequestBody CoatingPriceDto request) {
        return ResponseEntity.ok(customerService.getCoatingPrice(request));
    }
} 