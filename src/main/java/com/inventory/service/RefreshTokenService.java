package com.inventory.service;

import com.inventory.exception.ValidationException;
import com.inventory.security.JwtTokenProvider;
import com.inventory.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.inventory.entity.UserMaster;
import com.inventory.repository.UserMasterRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final UserMasterRepository userMasterRepository;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    
    @Transactional
    public Map<String, String> refreshToken(String refreshToken) {
        if (refreshToken == null) {
            throw new ValidationException("Refresh token is required");
        }
        
        UserMaster user = userMasterRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new ValidationException("Invalid refresh token"));
            
        if (user.getRefreshTokenExpiry().isBefore(OffsetDateTime.now())) {
            throw new ValidationException("Refresh token has expired");
        }
        
        // Create Authentication object
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            new UserPrincipal(user), null, new ArrayList<>()
        );
        
        // Generate new tokens
        String newJwtToken = tokenProvider.generateToken(authentication);
        String newRefreshToken = UUID.randomUUID().toString();
        
        // Update user with new refresh token
        user.setRefreshToken(newRefreshToken);
        user.setJwtToken(newJwtToken);
        userMasterRepository.save(user);
        
        Map<String, String> tokens = new HashMap<>();
        tokens.put("token", newJwtToken);
        tokens.put("refreshToken", newRefreshToken);
        
        return tokens;
    }
} 