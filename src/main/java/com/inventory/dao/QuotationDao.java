package com.inventory.dao;

import com.inventory.dto.QuotationDto;
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
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        
        sql.append("""
            SELECT 
                q.id, q.quote_number, q.quote_date, 
                q.total_amount, q.status, c.name as customer_name
            FROM (SELECT * FROM quotation q where q.client_id = :clientId) q
            LEFT JOIN (SELECT * FROM customer c where c.client_id = :clientId) c ON q.customer_id = c.id
        """);
        
        params.put("clientId", searchParams.getClientId());
        
        // Add search conditions and pagination
        appendSearchConditions(sql, params, searchParams);
        
        // Add hints for performance
        Query query = entityManager.createNativeQuery(sql.toString())
            .setHint(org.hibernate.annotations.QueryHints.FETCH_SIZE, 100)
            .setHint(org.hibernate.annotations.QueryHints.CACHEABLE, true);
            
        return executeQuery(query, params, searchParams);
    }

    private void appendSearchConditions(StringBuilder sql, Map<String, Object> params, QuotationDto searchParams) {
        if (searchParams.getSearch() != null && !searchParams.getSearch().trim().isEmpty()) {
            sql.append(" AND (q.quote_number LIKE :search OR c.name LIKE :search)");
            params.put("search", "%" + searchParams.getSearch().trim() + "%");
        }
        
        if (searchParams.getStartDate() != null) {
            sql.append(" AND q.quote_date >= :startDate");
            params.put("startDate", searchParams.getStartDate());
        }
        
        if (searchParams.getEndDate() != null) {
            sql.append(" AND q.quote_date <= :endDate");
            params.put("endDate", searchParams.getEndDate());
        }
        
        if (searchParams.getStatus() != null) {
            sql.append(" AND q.status = :status");
            params.put("status", searchParams.getStatus());
        }
        
        sql.append(" ORDER BY q.").append(searchParams.getSortBy()).append(" ")
           .append(searchParams.getSortDir())
           .append(" LIMIT :pageSize OFFSET :offset");
    }

    private Map<String, Object> executeQuery(Query query, Map<String, Object> params, QuotationDto searchParams) {
        params.forEach(query::setParameter);
        query.setParameter("pageSize", searchParams.getPerPageRecord());
        query.setParameter("offset", searchParams.getCurrentPage() * searchParams.getPerPageRecord());

        List<Object[]> results = query.getResultList();
        
        Query countQuery = entityManager.createNativeQuery("SELECT COUNT(*) FROM (" + query.unwrap(org.hibernate.query.Query.class).getQueryString() + ") x");
        params.forEach(countQuery::setParameter);
        Long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        return transformResults(results, totalRecords, searchParams.getPerPageRecord());
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
} 