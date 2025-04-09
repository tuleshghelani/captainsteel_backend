package com.inventory.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.inventory.dto.request.AttendanceDeleteRequestDto;
import com.inventory.dto.request.AttendanceRequestDto;
import com.inventory.dto.request.AttendanceSearchRequestDto;
import com.inventory.dto.request.AttendancePdfRequestDto;
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
    
    @PostMapping("/pdf")
    public ResponseEntity<byte[]> generateAttendancePdf(@RequestBody AttendancePdfRequestDto request) {
        byte[] pdfBytes = attendanceService.generateAttendancePdf(request);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
            .filename("attendance_.pdf")
            .build());
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
} 