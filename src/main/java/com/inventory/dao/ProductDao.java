package com.inventory.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.inventory.dto.ProductDto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class ProductDao {
    @PersistenceContext
    private EntityManager entityManager;
    
    public List<Map<String, Object>> getProducts(ProductDto productDto) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        sql.append("""
            SELECT 
                p.id,
                p.name,
                p.purchase_amount,
                p.sale_amount,
                p.type,
                p.weight,
                p.poly_carbonate_type,
                p.measurement
            FROM product p
            WHERE 1=1
        """);

        if (productDto != null) {
            if (StringUtils.hasText(productDto.getSearch())) {
                sql.append(" AND LOWER(p.name) LIKE LOWER(:name)");
                params.put("name", "%" + productDto.getSearch().trim() + "%");
            }
            if (StringUtils.hasText(productDto.getSearch())) {
                sql.append(" AND LOWER(p.description) LIKE LOWER(:description)");
                params.put("description", "%" + productDto.getSearch().trim() + "%");
            }
            
            if (StringUtils.hasText(productDto.getStatus())) {
                sql.append(" AND p.status = :status");
                params.put("status", productDto.getStatus().trim());
            }
            
            if (productDto.getCategoryId() != null) {
                sql.append(" AND p.category_id = :categoryId");
                params.put("categoryId", productDto.getCategoryId());
            }
        }
        
        sql.append(" AND p.client_id = :clientId");
        params.put("clientId", productDto.getClientId());
        
        sql.append(" ORDER BY p.id DESC");
        
        Query query = entityManager.createNativeQuery(sql.toString());
        
        params.forEach(query::setParameter);
        
        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> products = new ArrayList<>();
        
        for (Object[] row : results) {
            if (row[0] != null) {
                Map<String, Object> product = new HashMap<>(10);
                int index = 0;
                product.put("id", row[index++]);
                product.put("name", row[index++]);
                product.put("purchaseAmount", row[index++]);
                product.put("saleAmount", row[index++]);
                product.put("type", row[index++]);
                product.put("weight", row[index++]);
                product.put("polyCarbonateType", row[index++]);
                product.put("measurement", row[index++]);


                products.add(product);
            }
        }
        
        return products;
    }

    
    public Map<String, Object> searchProducts(ProductDto productDto) {
        // First get total count with a separate optimized query
        StringBuilder countSql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        countSql.append("""
            SELECT COUNT(p.id)
            FROM product p
            LEFT JOIN category c ON p.category_id = c.id
            WHERE 1=1
        """);

        appendSearchConditions(countSql, params, productDto);
        
        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        // Then get paginated data
        StringBuilder sql = new StringBuilder();
        
        sql.append("""
            SELECT 
                p.id,
                p.name,
                p.description,
                p.minimum_stock,
                p.status,
                p.remaining_quantity,
                c.id as category_id,
                c.name as category_name,
                p.purchase_amount,
                p.sale_amount,
                p.blocked_quantity,
                p.total_remaining_quantity,
                p.weight,
                p.type,
                p.poly_carbonate_type,
                p.measurement
            FROM product p
            LEFT JOIN category c ON p.category_id = c.id
            WHERE 1=1
        """);

        appendSearchConditions(sql, params, productDto);

        sql.append("""
                ORDER BY p.%s %s
                LIMIT :pageSize OFFSET :offset
            """.formatted(productDto.getSortBy(), productDto.getSortDir().toUpperCase()));

        Query query = entityManager.createNativeQuery(sql.toString());
        setQueryParameters(query, params, productDto);

        List<Object[]> results = query.getResultList();

        return transformResults(results, totalRecords, productDto);
    }

    private void appendSearchConditions(StringBuilder sql, Map<String, Object> params, ProductDto productDto) {
        if (productDto != null) {
            if (StringUtils.hasText(productDto.getSearch())) {
                sql.append(" AND (LOWER(p.name) LIKE LOWER(:search) OR LOWER(p.description) LIKE LOWER(:search))");
                params.put("search", "%" + productDto.getSearch().trim() + "%");
            }

            if (StringUtils.hasText(productDto.getStatus())) {
                sql.append(" AND p.status = :status");
                params.put("status", productDto.getStatus().trim());
            }

            if (productDto.getCategoryId() != null) {
                sql.append(" AND p.category_id = :categoryId");
                params.put("categoryId", productDto.getCategoryId());
            }
        }

        sql.append(" AND p.client_id = :clientId");
        params.put("clientId", productDto.getClientId());
    }

    private void setQueryParameters(Query query, Map<String, Object> params, ProductDto productDto) {
        params.forEach(query::setParameter);
        query.setParameter("pageSize", productDto.getSize());
        query.setParameter("offset", productDto.getPage() * productDto.getSize());
    }

    private Map<String, Object> transformResults(List<Object[]> results, long totalRecords, ProductDto productDto) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> products = new ArrayList<>();

        for (Object[] row : results) {
            if (row[0] != null) {
                int index = 0;
                Map<String, Object> product = new HashMap<>(12);
                product.put("id", row[index++]);
                product.put("name", row[index++]);
                product.put("description", row[index++]);
                product.put("minimumStock", row[index++]);
                product.put("status", row[index++]);
                product.put("remainingQuantity", row[index++]);
                product.put("categoryId", row[index++]);
                product.put("categoryName", row[index++]);
                product.put("purchaseAmount", row[index++]);
                product.put("saleAmount", row[index++]);
                product.put("blockedQuantity", row[index++]);
                product.put("totalRemainingQuantity", row[index++]);
                product.put("weight", row[index++]);
                product.put("type", row[index++]);
                product.put("polyCarbonateType", row[index++]);
                product.put("measurement", row[index++]);
                products.add(product);
            }
        }

        response.put("content", products);
        response.put("totalElements", totalRecords);
        int pageSize =  productDto.getSize();
        response.put("totalPages", (int) Math.ceil((double) totalRecords / pageSize));

        return response;
    }

    public List<Map<String, Object>> getProductsForPdf(ProductDto productDto) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        sql.append("""
            SELECT 
                p.id,
                p.name,
                p.description,
                p.minimum_stock,
                p.status,
                p.remaining_quantity,
                c.id as category_id,
                c.name as category_name,
                p.purchase_amount,
                p.sale_amount,
                p.blocked_quantity,
                p.total_remaining_quantity,
                p.weight,
                p.type,
                p.poly_carbonate_type,
                p.measurement
            FROM product p
            LEFT JOIN category c ON p.category_id = c.id
            WHERE 1=1
        """);

        appendSearchConditions(sql, params, productDto);
        sql.append(" ORDER BY p.").append(productDto.getSortBy()).append(" ").append(productDto.getSortDir().toUpperCase());

        Query query = entityManager.createNativeQuery(sql.toString())
            .setHint(org.hibernate.annotations.QueryHints.FETCH_SIZE, 100)
            .setHint(org.hibernate.annotations.QueryHints.CACHEABLE, true);
        
        params.forEach(query::setParameter);

        List<Object[]> results = query.getResultList();
        return transformPdfResults(results);
    }

    private List<Map<String, Object>> transformPdfResults(List<Object[]> results) {
        List<Map<String, Object>> products = new ArrayList<>();

        for (Object[] row : results) {
            if (row[0] != null) {
                int index = 0;
                Map<String, Object> product = new HashMap<>(12);
                product.put("id", row[index++]);
                product.put("name", row[index++]);
                product.put("description", row[index++]);
                product.put("minimumStock", row[index++]);
                product.put("status", row[index++]);
                product.put("remainingQuantity", row[index++]);
                product.put("categoryId", row[index++]);
                product.put("categoryName", row[index++]);
                product.put("purchaseAmount", row[index++]);
                product.put("saleAmount", row[index++]);
                product.put("blockedQuantity", row[index++]);
                product.put("totalRemainingQuantity", row[index++]);
                product.put("weight", row[index++]);
                product.put("type", row[index++]);
                product.put("polyCarbonateType", row[index++]);
                product.put("measurement", row[index++]);
                products.add(product);
            }
        }

        return products;
    }
}