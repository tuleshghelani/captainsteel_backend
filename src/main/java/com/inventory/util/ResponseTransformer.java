package com.inventory.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ResponseTransformer {
    public static Map<String, Object> createPageResponse(List<?> content, long totalElements, int pageSize) {
        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("totalElements", totalElements);
        response.put("totalPages", (totalElements + pageSize - 1) / pageSize);
        return response;
    }
    
    public static Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", message);
        response.put("data", data);
        return response;
    }
} 