package com.inventory.controller;

import com.inventory.dto.EmployeeDto;
import com.inventory.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody EmployeeDto request) {
        return ResponseEntity.ok(employeeService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody EmployeeDto request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.delete(id));
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchEmployees(@RequestBody EmployeeDto request) {
        return ResponseEntity.ok(employeeService.searchEmployees(request));
    }

    @PostMapping("/detail")
    public ResponseEntity<?> getEmployeeDetail(@RequestBody EmployeeDto request) {
        return ResponseEntity.ok(employeeService.getEmployeeDetail(request));
    }

    @PostMapping("/all")
    public ResponseEntity<?> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }
} 