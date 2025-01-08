package com.inventory.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.inventory.dto.request.AttendanceDeleteRequestDto;
import com.inventory.dto.request.AttendanceRequestDto;
import com.inventory.dto.request.AttendanceSearchRequestDto;
import com.inventory.service.AttendanceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;
    
    @PostMapping("/create")
    public ResponseEntity<?> saveAttendance(@RequestBody AttendanceRequestDto request) {
        return ResponseEntity.ok(attendanceService.saveAttendance(request));
    }
    
    @PostMapping("/search")
    public ResponseEntity<?> getAttendanceByEmployee(@RequestBody AttendanceSearchRequestDto request) {
        return ResponseEntity.ok(attendanceService.getAttendanceByEmployee(request));
    }
    
    @PostMapping("/delete")
    public ResponseEntity<?> deleteAttendances(@RequestBody AttendanceDeleteRequestDto request) {
        return ResponseEntity.ok(attendanceService.deleteAttendances(request));
    }
} 