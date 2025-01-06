
package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.JwtAuthenticationResponse;
import com.inventory.dto.LoginRequest;
import com.inventory.dto.RegisterRequest;
import com.inventory.exception.ValidationException;
import com.inventory.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) throws ValidationException {
        return ResponseEntity.ok(authService.login(request));
    }
}
