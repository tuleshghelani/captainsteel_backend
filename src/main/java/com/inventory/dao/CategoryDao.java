package com.inventory.dao;

import com.inventory.dto.CategoryDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class CategoryDao {
    @PersistenceContext
    private EntityManager entityManager;
    public List<Map<String, Object>> getCategories(CategoryDto categoryDto) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        sql.append("""
            SELECT 
                c.id,
                c.name,
                c.status,
                c.created_at as createdAt,
                c.updated_at as updatedAt,
                u.first_name || ' ' || u.last_name as createdBy
            FROM category c
            LEFT JOIN user_master u ON c.created_by = u.id
            WHERE 1=1
        """);

        // Add search conditions
        if (categoryDto != null) {
            if (StringUtils.hasText(categoryDto.getName())) {
                sql.append(" AND LOWER(c.name) LIKE LOWER(:name)");
                params.put("name", "%" + categoryDto.getName().trim() + "%");
            }

            if (StringUtils.hasText(categoryDto.getStatus())) {
                sql.append(" AND c.status = :status");
                params.put("status", categoryDto.getStatus().trim());
            }
        }

        sql.append(" AND c.client_id = :clientId");
        params.put("clientId", categoryDto.getClientId());

        sql.append(" ORDER BY c.id DESC");

        Query query = entityManager.createNativeQuery(sql.toString());

        // Set parameters
        params.forEach(query::setParameter);

        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> categories = new ArrayList<>();

        for (Object[] row : results) {
            if (row[0] != null) { // Only process if ID exists
                Map<String, Object> category = new HashMap<>(6);
                category.put("id", row[0]);
                category.put("name", row[1]);
                category.put("status", row[2]);
                category.put("createdAt", row[3]);
                category.put("updatedAt", row[4]);

                // Only add createdBy if it's not empty
                String createdBy = (String) row[5];
                if (StringUtils.hasText(createdBy)) {
                    category.put("createdBy", createdBy.trim());
                }

                categories.add(category);
            }
        }

        return categories;
    }

    public Map<String, Object> searchCategories(CategoryDto categoryDto) {
        // First get total count with a separate optimized query
        StringBuilder countSql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        countSql.append("""
            SELECT COUNT(c.id)
            FROM category c
            LEFT JOIN user_master u ON c.created_by = u.id
            WHERE c.client_id = :clientId
        """);

        appendSearchConditions(countSql, params, categoryDto);
        
        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        // Then get paginated data
        StringBuilder sql = new StringBuilder();
        
        sql.append("""
            SELECT
                c.id,
                c.name,
                c.status,
                c.remaining_quantity
            FROM category c
            LEFT JOIN user_master u ON c.created_by = u.id
            WHERE c.client_id = :clientId
        """);

        appendSearchConditions(sql, params, categoryDto);

        sql.append("""
                ORDER BY c.%s %s
                LIMIT :pageSize OFFSET :offset
            """.formatted(categoryDto.getSortBy(), categoryDto.getSortDir().toUpperCase()));

        Query query = entityManager.createNativeQuery(sql.toString());
        setQueryParameters(query, params, categoryDto);

        List<Object[]> results = query.getResultList();

        return transformResults(results, totalRecords, categoryDto);
    }

    private void appendSearchConditions(StringBuilder sql, Map<String, Object> params, CategoryDto categoryDto) {
        if (categoryDto != null) {
            if (StringUtils.hasText(categoryDto.getSearch())) {
                sql.append(" AND LOWER(c.name) LIKE LOWER(:name)");
                params.put("name", "%" + categoryDto.getSearch().trim() + "%");
            }

            if (StringUtils.hasText(categoryDto.getStatus())) {
                sql.append(" AND c.status = :status");
                params.put("status", categoryDto.getStatus().trim());
            }
        }
        sql.append(" AND c.client_id = :clientId");
        params.put("clientId", categoryDto.getClientId());
    }

    private void setQueryParameters(Query query, Map<String, Object> params, CategoryDto categoryDto) {
        params.forEach(query::setParameter);
        query.setParameter("pageSize", categoryDto.getSize());
        query.setParameter("offset", categoryDto.getPage() * categoryDto.getSize());
    }

    private Map<String, Object> transformResults(List<Object[]> results, long totalRecords, CategoryDto categoryDto) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> categories = new ArrayList<>();

        for (Object[] row : results) {
            if (row[0] != null) {
                Map<String, Object> category = new HashMap<>(7);
                category.put("id", row[0]);
                category.put("name", row[1]);
                category.put("status", row[2]);
                category.put("remainingQuantity", row[3]);
                categories.add(category);
            }
        }

        response.put("content", categories);
        response.put("totalElements", totalRecords);
        int pageSize =  categoryDto.getSize();
        response.put("totalPages", (int) Math.ceil((double) totalRecords / pageSize));

        return response;
    }
}