package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.RefreshTokenRequestDto;
import com.inventory.exception.ValidationException;
import com.inventory.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/refresh-token")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class RefreshTokenController {
    private final RefreshTokenService refreshTokenService;
    private final Logger logger = LoggerFactory.getLogger(RefreshTokenController.class);

    @PostMapping("/new")
    public ResponseEntity<ApiResponse<?>> refreshToken(@RequestBody RefreshTokenRequestDto request) {
        try {
            logger.info("Received refresh token request");
            Map<String, String> tokens = refreshTokenService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", tokens));
        } catch (ValidationException ve) {
            logger.error("Validation error in refreshToken: {}", ve.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(ve.getMessage()));
        } catch (Exception e) {
            logger.error("Error in refreshToken: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid or expired refresh token"));
        }
    }
} 