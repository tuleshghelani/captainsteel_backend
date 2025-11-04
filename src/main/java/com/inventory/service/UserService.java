package com.inventory.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.inventory.dao.UserDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.UserDto;
import com.inventory.entity.Client;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.ClientRepository;
import com.inventory.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final UserDao userDao;
    private final UtilityService utilityService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Check if current user has ADMIN role
     */
    private void validateAdminRole() throws ValidationException {
        UserMaster currentUser = utilityService.getCurrentLoggedInUser();
        if (currentUser.getRoles() == null || !currentUser.getRoles().contains("ADMIN")) {
            throw new ValidationException("Access denied. ADMIN role required.", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Create a new user (ADMIN only)
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> createUser(UserDto request) {
        try {
            validateAdminRole();
            validateUserRequest(request, true);

            // Check if email already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ValidationException("User with this email already exists", HttpStatus.CONFLICT);
            }

            UserMaster user = new UserMaster();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "A");
            user.setIsSystem(request.getIsSystem() != null ? request.getIsSystem() : false);
            
            // Set roles
            if (request.getRoles() != null && !request.getRoles().isEmpty()) {
                user.setRoles(request.getRoles());
            } else {
                user.setRoles(new ArrayList<>());
            }

            // Set client
            if (request.getClientId() != null) {
                Client client = clientRepository.findById(request.getClientId())
                        .orElseThrow(() -> new ValidationException("Client not found", HttpStatus.NOT_FOUND));
                user.setClient(client);
            }

            user.setCreatedAt(OffsetDateTime.now());
            user.setUpdatedAt(OffsetDateTime.now());
            
            userRepository.save(user);
            log.info("User created successfully with email: {}", request.getEmail());
            return ApiResponse.success("User created successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage(), e);
            throw new ValidationException("Failed to create user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update user password and/or status (ADMIN only)
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> updateUserPassword(UserDto request) {
        try {
            validateAdminRole();
            
            if (request.getId() == null) {
                throw new ValidationException("User ID is required", HttpStatus.BAD_REQUEST);
            }

            UserMaster user = userRepository.findById(request.getId())
                    .orElseThrow(() -> new ValidationException("User not found", HttpStatus.NOT_FOUND));

            // Update password if provided
            if (StringUtils.hasText(request.getPassword())) {
//                if (request.getPassword().length() < 6) {
//                    throw new ValidationException("Password must be at least 6 characters long", HttpStatus.BAD_REQUEST);
//                }
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                log.info("Password updated for user ID: {}", request.getId());
            }

            // Update status if provided
            if (StringUtils.hasText(request.getStatus())) {
                if (!request.getStatus().equals("A") && !request.getStatus().equals("I")) {
                    throw new ValidationException("Status must be 'A' (Active) or 'I' (Inactive)", HttpStatus.BAD_REQUEST);
                }
                user.setStatus(request.getStatus());
                log.info("Status updated for user ID: {} to {}", request.getId(), request.getStatus());
            }

            user.setJwtToken(null);
            user.setRefreshToken(null);
            if (StringUtils.hasText(request.getPassword()) || StringUtils.hasText(request.getStatus())) {
                user.setUpdatedAt(OffsetDateTime.now());
                userRepository.save(user);
                return ApiResponse.success("User updated successfully");
            } else {
                throw new ValidationException("Either password or status must be provided", HttpStatus.BAD_REQUEST);
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage(), e);
            throw new ValidationException("Failed to update user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Search users by client_id and isSystem=false (ADMIN only)
     */
    public ApiResponse<Map<String, Object>> searchUsers(UserDto dto) {
        try {
            validateAdminRole();
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            
            // Set pagination defaults
            if (dto.getPage() == null) {
                dto.setPage(0);
            }
            if (dto.getSize() == null) {
                dto.setSize(10);
            }
            if (!StringUtils.hasText(dto.getSortBy())) {
                dto.setSortBy("id");
            }
            if (!StringUtils.hasText(dto.getSortDir())) {
                dto.setSortDir("desc");
            }

            Map<String, Object> result = userDao.searchUsers(dto);
            return ApiResponse.success("Users retrieved successfully", result);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error searching users: {}", e.getMessage(), e);
            throw new ValidationException("Failed to search users: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get single user full data by ID (ADMIN only)
     */
    public ApiResponse<?> getUserDetail(UserDto dto) {
        try {
            validateAdminRole();
            
            if (dto.getId() == null) {
                throw new ValidationException("User ID is required", HttpStatus.BAD_REQUEST);
            }

            UserMaster user = userRepository.findById(dto.getId())
                    .orElseThrow(() -> new ValidationException("User not found", HttpStatus.NOT_FOUND));

            // Map entity to DTO
            UserDto userDto = new UserDto();
            userDto.setId(user.getId());
            userDto.setEmail(user.getEmail());
            userDto.setFirstName(user.getFirstName());
            userDto.setLastName(user.getLastName());
            userDto.setStatus(user.getStatus());
            userDto.setIsSystem(user.getIsSystem());
            userDto.setRoles(user.getRoles());
            userDto.setCreatedAt(user.getCreatedAt());
            userDto.setUpdatedAt(user.getUpdatedAt());
            
            if (user.getClient() != null) {
                userDto.setClientId(user.getClient().getId());
                userDto.setClientName(user.getClient().getName());
            }

            log.info("User detail retrieved successfully for user ID: {}", dto.getId());
            return ApiResponse.success("User detail retrieved successfully", userDto);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting user detail: {}", e.getMessage(), e);
            throw new ValidationException("Failed to get user detail: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Validate user request data
     */
    private void validateUserRequest(UserDto request, boolean isCreate) {
        if (isCreate) {
            if (!StringUtils.hasText(request.getEmail())) {
                throw new ValidationException("Email is required", HttpStatus.BAD_REQUEST);
            }
            if (!StringUtils.hasText(request.getPassword())) {
                throw new ValidationException("Password is required", HttpStatus.BAD_REQUEST);
            }
            if (request.getPassword().length() < 6) {
                throw new ValidationException("Password must be at least 6 characters long", HttpStatus.BAD_REQUEST);
            }
        }

        if (request.getEmail() != null && !isValidEmail(request.getEmail())) {
            throw new ValidationException("Invalid email format", HttpStatus.BAD_REQUEST);
        }

        if (request.getStatus() != null && !request.getStatus().equals("A") && !request.getStatus().equals("I")) {
            throw new ValidationException("Status must be 'A' (Active) or 'I' (Inactive)", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Basic email validation
     */
    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
}
