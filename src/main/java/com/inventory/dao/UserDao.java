package com.inventory.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.inventory.dto.UserDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class UserDao {
    @PersistenceContext
    private EntityManager entityManager;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public Map<String, Object> searchUsers(UserDto dto) {
        StringBuilder countSql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        countSql.append("""
            SELECT COUNT(um.id) 
            FROM "user_master" um
            LEFT JOIN client c ON um.client_id = c.id
            WHERE 1=1
        """);
        appendSearchConditions(countSql, params, dto);

        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT 
                um.id,
                um.email,
                um.first_name,
                um.last_name,
                um.status,
                um.is_system,
                um.roles,
                um.client_id,
                c.name as client_name,
                um.created_at,
                um.updated_at
            FROM "user_master" um
            LEFT JOIN client c ON um.client_id = c.id
            WHERE 1=1
        """);

        appendSearchConditions(sql, params, dto);
        sql.append("""
            ORDER BY um.%s %s
            LIMIT :pageSize OFFSET :offset
        """.formatted(dto.getSortBy(), dto.getSortDir().toUpperCase()));

        Query query = entityManager.createNativeQuery(sql.toString());
        setQueryParameters(query, params, dto);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        return transformResults(results, totalRecords, dto.getSize());
    }

    private void appendSearchConditions(StringBuilder sql, Map<String, Object> params, UserDto dto) {
        // Always filter by isSystem=false
        sql.append(" AND um.is_system = false");
        
        // Filter by client_id if provided
        if (dto.getClientId() != null) {
            sql.append(" AND um.client_id = :clientId");
            params.put("clientId", dto.getClientId());
        }
        
        // Search by email, first name, or last name
        if (StringUtils.hasText(dto.getSearch())) {
            sql.append(" AND (LOWER(um.email) LIKE LOWER(:search) OR LOWER(um.first_name) LIKE LOWER(:search) OR LOWER(um.last_name) LIKE LOWER(:search))");
            params.put("search", "%" + dto.getSearch().trim() + "%");
        }
        
        // Filter by status if provided
        if (StringUtils.hasText(dto.getStatus())) {
            sql.append(" AND um.status = :status");
            params.put("status", dto.getStatus().trim());
        }
    }

    private void setQueryParameters(Query query, Map<String, Object> params, UserDto dto) {
        params.forEach(query::setParameter);
        query.setParameter("pageSize", dto.getSize());
        query.setParameter("offset", dto.getPage() * dto.getSize());
    }

    private Map<String, Object> transformResults(List<Object[]> results, long totalRecords, int pageSize) {
        List<Map<String, Object>> users = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> user = new HashMap<>();
            int index = 0;
            user.put("id", row[index++]);
            user.put("email", row[index++]);
            user.put("firstName", row[index++]);
            user.put("lastName", row[index++]);
            user.put("status", row[index++]);
            user.put("isSystem", row[index++]);
            user.put("roles", parseRoles(row[index++]));
            user.put("clientId", row[index++]);
            user.put("clientName", row[index++]);
            user.put("createdAt", row[index++]);
            user.put("updatedAt", row[index++]);
            users.add(user);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", users);
        response.put("totalElements", totalRecords);
        response.put("totalPages", (int) Math.ceil((double) totalRecords / pageSize));

        return response;
    }

    private List<String> parseRoles(Object rolesValue) {
        // Handle null case
        if (rolesValue == null) {
            return new ArrayList<>();
        }

        // If it's already a List, return it as is
        if (rolesValue instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> rolesList = (List<String>) rolesValue;
            return rolesList != null ? rolesList : new ArrayList<>();
        }

        // If it's a string, parse it
        if (rolesValue instanceof String) {
            String rolesStr = ((String) rolesValue).trim();
            
            // Handle empty string case
            if (rolesStr.isEmpty() || rolesStr.equals("[]") || rolesStr.equals("null")) {
                return new ArrayList<>();
            }

            try {
                // Remove escaped quotes if present (e.g., [\"ADMIN\"] -> ["ADMIN"])
                rolesStr = rolesStr.replace("\\\"", "\"");
                
                // Parse the JSON string to List<String>
                return OBJECT_MAPPER.readValue(rolesStr, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                // If parsing fails, return empty list
                return new ArrayList<>();
            }
        }

        // For any other type, return empty list
        return new ArrayList<>();
    }
}

