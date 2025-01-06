package com.inventory.service;

import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UtilityService {

    @Autowired
    private UserRepository userRepository;


    public UserMaster getCurrentLoggedInUser() throws ValidationException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                throw new ValidationException("No authentication found", HttpStatus.UNAUTHORIZED);
            }

            String username = authentication.getName();
            if (username == null || username.trim().isEmpty()) {
                throw new ValidationException("No username found in authentication", HttpStatus.UNAUTHORIZED);
            }

            Optional<UserMaster> userByEmail = userRepository.findByEmail(username);
            return userByEmail.get();
        } catch (ValidationException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Error getting current user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
