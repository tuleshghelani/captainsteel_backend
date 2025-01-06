package com.inventory.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.inventory.exception.ValidationException;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.ApiResponse;
import java.io.IOException;
import com.inventory.entity.UserMaster;
import com.inventory.repository.UserRepository;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private JwtTokenProvider tokenProvider;
    private CustomUserDetailsService customUserDetailsService;
    private ObjectMapper objectMapper = new ObjectMapper();
    private UserRepository userRepository;
    
    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, CustomUserDetailsService customUserDetailsService, UserRepository userRepository) {
        this.tokenProvider = tokenProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.userRepository = userRepository;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                Long userId = tokenProvider.getUserIdFromJWT(jwt);
                
                // Verify token matches database
                UserMaster user = userRepository.findById(userId)
                    .orElseThrow(() -> new ValidationException("User not found", HttpStatus.UNAUTHORIZED));
                    
                if (!jwt.equals(user.getJwtToken())) {
                    throw new ValidationException("Invalid token", HttpStatus.UNAUTHORIZED);
                }
                
                UserDetails userDetails = customUserDetailsService.loadUserById(userId);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (ValidationException ex) {
            handleAuthenticationError(response, ex.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (Exception ex) {
            handleAuthenticationError(response, "Invalid authentication token", HttpStatus.UNAUTHORIZED);
        }
    }
    
    private void handleAuthenticationError(HttpServletResponse response, String message, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        ApiResponse<?> apiResponse = ApiResponse.error(message);
        String jsonResponse = objectMapper.writeValueAsString(apiResponse);
        response.getWriter().write(jsonResponse);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}