package com.inventory.exception;

import com.inventory.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Unexpected error: ", e);
        return ResponseEntity.ok(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(ValidationException e) {
        log.error("Validation error: ", e);
        return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
    }
} 