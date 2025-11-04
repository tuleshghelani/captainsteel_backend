package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.UserDto;
import com.inventory.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    /**
     * Create a new user
     * Only accessible by ADMIN role
     * 
     * POST /api/users/create
     * 
     * Request Body:
     * {
     *   "email": "user@example.com",
     *   "password": "password123",
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "status": "A",
     *   "clientId": 1,
     *   "roles": ["USER"],
     *   "isSystem": false
     * }
     * 
     * Response:
     * {
     *   "success": true,
     *   "message": "User created successfully",
     *   "data": null
     * }
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<?>> createUser(@RequestBody UserDto request) {
        log.debug("Received create user request for email: {}", request.getEmail());
        return ResponseEntity.ok(userService.createUser(request));
    }

    /**
     * Update user password and/or status
     * Only accessible by ADMIN role
     * 
     * PUT /api/users/update-password
     * 
     * Request Body:
     * {
     *   "id": 1,
     *   "password": "newPassword123",
     *   "status": "A"
     * }
     * 
     * Response:
     * {
     *   "success": true,
     *   "message": "User updated successfully",
     *   "data": null
     * }
     */
    @PutMapping("/update-password")
    public ResponseEntity<ApiResponse<?>> updateUserPassword(@RequestBody UserDto request) {
        log.debug("Received update user password request for user ID: {}", request.getId());
        return ResponseEntity.ok(userService.updateUserPassword(request));
    }

    /**
     * Search/list users by client_id and isSystem=false
     * Only accessible by ADMIN role
     * 
     * POST /api/users/search
     * 
     * Request Body:
     * {
     *   "clientId": 1,
     *   "search": "john",
     *   "status": "A",
     *   "page": 0,
     *   "size": 10,
     *   "sortBy": "id",
     *   "sortDir": "desc"
     * }
     * 
     * Response:
     * {
     *   "success": true,
     *   "message": "Users retrieved successfully",
     *   "data": {
     *     "content": [
     *       {
     *         "id": 1,
     *         "email": "user@example.com",
     *         "firstName": "John",
     *         "lastName": "Doe",
     *         "status": "A",
     *         "isSystem": false,
     *         "roles": ["USER"],
     *         "clientId": 1,
     *         "clientName": "Client Name",
     *         "createdAt": "01-01-2024 10:00:00",
     *         "updatedAt": "01-01-2024 10:00:00"
     *       }
     *     ],
     *     "totalElements": 100,
     *     "totalPages": 10
     *   }
     * }
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<?>> searchUsers(@RequestBody UserDto request) {
        log.debug("Received search users request: clientId={}, search={}, page={}, size={}", 
            request.getClientId(), request.getSearch(), request.getPage(), request.getSize());
        return ResponseEntity.ok(userService.searchUsers(request));
    }

    /**
     * Get single user full data by ID
     * Only accessible by ADMIN role
     * 
     * POST /api/users/detail
     * 
     * Request Body:
     * {
     *   "id": 1
     * }
     * 
     * Response:
     * {
     *   "success": true,
     *   "message": "User detail retrieved successfully",
     *   "data": {
     *     "id": 1,
     *     "email": "user@example.com",
     *     "firstName": "John",
     *     "lastName": "Doe",
     *     "status": "A",
     *     "isSystem": false,
     *     "roles": ["USER"],
     *     "clientId": 1,
     *     "clientName": "Client Name",
     *     "createdAt": "01-01-2024 10:00:00",
     *     "updatedAt": "01-01-2024 10:00:00"
     *   }
     * }
     */
    @PostMapping("/detail")
    public ResponseEntity<ApiResponse<?>> getUserDetail(@RequestBody UserDto request) {
        log.debug("Received get user detail request for user ID: {}", request.getId());
        return ResponseEntity.ok(userService.getUserDetail(request));
    }
}

