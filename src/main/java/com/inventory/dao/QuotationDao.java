package com.inventory.dao;

import com.inventory.dto.QuotationDto;
import com.inventory.exception.ValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class QuotationDao {
    @PersistenceContext
    private EntityManager entityManager;
    
    public Map<String, Object> searchQuotations(QuotationDto searchParams) {
        Map<String, Object> params = new HashMap<>();
        params.put("clientId", searchParams.getClientId());
        
        StringBuilder nativeQuery = buildNativeQuery();
        StringBuilder conditions = buildSearchConditions(params, searchParams);
        
        // Count Query
        String countSql = buildCountQuery(nativeQuery.toString(), conditions.toString());
        Query countQuery = entityManager.createNativeQuery(countSql);
        setQueryParameters(countQuery, params, null);
        Long totalRecords = ((Number) countQuery.getSingleResult()).longValue();
        
        // Main Query with pagination
        String mainSql = buildMainQuery(nativeQuery.toString(), conditions.toString(), searchParams);
        Query query = entityManager.createNativeQuery(mainSql)
            .setHint(org.hibernate.annotations.QueryHints.FETCH_SIZE, 100)
            .setHint(org.hibernate.annotations.QueryHints.CACHEABLE, true);
        setQueryParameters(query, params, searchParams);
        
        List<Object[]> results = query.getResultList();
        return transformResults(results, totalRecords, searchParams.getPerPageRecord());
    }

    private StringBuilder buildNativeQuery() {
        return new StringBuilder("""
            FROM (SELECT * FROM quotation q WHERE q.client_id = :clientId) q
            LEFT JOIN (SELECT * FROM customer c WHERE c.client_id = :clientId) c 
            ON q.customer_id = c.id
            WHERE 1=1
            """);
    }

    private String buildMainQuery(String nativeQuery, String conditions, QuotationDto searchParams) {
        return new StringBuilder()
            .append("SELECT q.id, q.quote_number, q.quote_date,")
            .append(" q.total_amount, q.status, c.name as customer_name ")
            .append(nativeQuery)
            .append(conditions)
            .append(" ORDER BY q.").append(searchParams.getSortBy()).append(" ")
            .append(searchParams.getSortDir())
            .append(" LIMIT :pageSize OFFSET :offset")
            .toString();
    }

    private String buildCountQuery(String nativeQuery, String conditions) {
        return new StringBuilder()
            .append("SELECT COUNT(*) ")
            .append(nativeQuery)
            .append(conditions)
            .toString();
    }

    private StringBuilder buildSearchConditions(Map<String, Object> params, QuotationDto searchParams) {
        StringBuilder conditions = new StringBuilder();
        
        if (searchParams.getSearch() != null && !searchParams.getSearch().trim().isEmpty()) {
            conditions.append(" AND (q.quote_number LIKE :search OR c.name LIKE :search)");
            params.put("search", "%" + searchParams.getSearch().trim() + "%");
        }
        if (searchParams.getStartDate() != null) {
            conditions.append(" AND q.quote_date >= :startDate");
            params.put("startDate", searchParams.getStartDate());
        }
        if (searchParams.getEndDate() != null) {
            conditions.append(" AND q.quote_date <= :endDate");
            params.put("endDate", searchParams.getEndDate());
        }
        if (searchParams.getStatus() != null) {
            conditions.append(" AND q.status = :status");
            params.put("status", searchParams.getStatus());
        }
        
        return conditions;
    }

    private void setQueryParameters(Query query, Map<String, Object> params, QuotationDto searchParams) {
        params.forEach((key, value) -> query.setParameter(key, value));
        if (searchParams != null) {
            query.setParameter("pageSize", searchParams.getPerPageRecord());
            query.setParameter("offset", searchParams.getCurrentPage() * searchParams.getPerPageRecord());
        }
    }

    private Map<String, Object> transformResults(List<Object[]> results, Long totalRecords, Integer pageSize) {
        List<Map<String, Object>> quotations = new ArrayList<>();
        
        for (Object[] row : results) {
            Map<String, Object> quotation = new HashMap<>();
            quotation.put("id", row[0]);
            quotation.put("quoteNumber", row[1]);
            quotation.put("quoteDate", row[2]);
            quotation.put("totalAmount", row[3]);
            quotation.put("status", row[4]);
            quotation.put("customerName", row[5]);
            quotations.add(quotation);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("quotations", quotations);
        response.put("totalRecords", totalRecords);
        response.put("pageSize", pageSize);
        
        return response;
    }

    public Map<String, Object> getQuotationDetail(QuotationDto request) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                q.id, q.quote_number, q.quote_date, q.valid_until,
                q.total_amount, q.status, q.remarks, q.terms_conditions,
                c.id as customer_id, q.customer_name,
                qi.id as item_id, qi.quantity, qi.unit_price,
                qi.discount_percentage, qi.discount_amount,
                qi.tax_percentage, qi.tax_amount, qi.final_price,
                p.id as product_id, p.name as product_name
            FROM quotation q
            LEFT JOIN customer c ON q.customer_id = c.id
            LEFT JOIN quotation_items qi ON q.id = qi.quotation_id
            LEFT JOIN product p ON qi.product_id = p.id
            WHERE q.id = :quotationId 
            AND q.client_id = :clientId
        """);

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("quotationId", request.getId());
        query.setParameter("clientId", request.getClientId());

        List<Object[]> results = query.getResultList();
        return transformDetailResults(results);
    }

    private Map<String, Object> transformDetailResults(List<Object[]> results) {
        if (results.isEmpty()) {
            throw new ValidationException("Quotation not found");
        }

        Map<String, Object> quotation = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();

        // Set quotation details from first row
        Object[] firstRow = results.get(0);
        quotation.put("id", firstRow[0]);
        quotation.put("quoteNumber", firstRow[1]);
        quotation.put("quoteDate", firstRow[2]);
        quotation.put("validUntil", firstRow[3]);
        quotation.put("totalAmount", firstRow[4]);
        quotation.put("status", firstRow[5]);
        quotation.put("remarks", firstRow[6]);
        quotation.put("termsConditions", firstRow[7]);
        quotation.put("customerId", firstRow[8]);
        quotation.put("customerName", firstRow[9]);

        // Process items
        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row[10]);
            item.put("quantity", row[11]);
            item.put("unitPrice", row[12]);
            item.put("discountPercentage", row[13]);
            item.put("discountAmount", row[14]);
            item.put("taxPercentage", row[15]);
            item.put("taxAmount", row[16]);
            item.put("finalPrice", row[17]);
            item.put("productId", row[18]);
            item.put("productName", row[19]);
            items.add(item);
        }

        quotation.put("items", items);
        return quotation;
    }
} 