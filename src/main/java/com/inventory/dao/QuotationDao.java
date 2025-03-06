package com.inventory.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.inventory.dto.QuotationDto;
import com.inventory.exception.ValidationException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

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
            .append(" q.total_amount, q.status, COALESCE(c.name, q.customer_name, '') as customer_name, ")
            .append(" q.valid_until, q.remarks, q.terms_conditions ")
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
        if(searchParams.getCustomerId() != null) {
            conditions.append(" AND q.customer_id = :customerId");
            params.put("customerId", searchParams.getCustomerId());
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
            int index = 0;
            quotation.put("id", row[index++]);
            quotation.put("quoteNumber", row[index++]);
            quotation.put("quoteDate", row[index++]);
            quotation.put("totalAmount", row[index++]);
            quotation.put("status", row[index++]);
            quotation.put("customerName", row[index++]);
            quotation.put("validUntil", row[index++]);
            quotation.put("remarks", row[index++]);
            quotation.put("termsConditions", row[index++]);
            quotations.add(quotation);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", quotations);
        response.put("totalElements", totalRecords);
        response.put("pageSize", pageSize);
        response.put("totalPages", (totalRecords + pageSize - 1) / pageSize);

        return response;
    }

    public Map<String, Object> getQuotationDetail(QuotationDto request) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                q.id, q.quote_number, q.quote_date, q.valid_until,
                q.total_amount, q.status, q.remarks, q.terms_conditions,
                c.id as customer_id, q.customer_name, q.contact_number, q.loading_charge,
                qi.id as item_id, qi.quantity, qi.unit_price,
                qi.discount_percentage, qi.discount_amount,
                qi.tax_percentage, qi.tax_amount, qi.final_price,
                p.id as product_id, p.name as product_name, p.type, qi.calculation_type,
                qi.discount_price, p.measurement
            FROM (select * from quotation q where q.client_id = :clientId and q.id = :quotationId) q
            LEFT JOIN (select * from customer c where c.client_id = :clientId) c ON q.customer_id = c.id
            LEFT JOIN (select * from quotation_items qi where qi.client_id = :clientId) qi ON q.id = qi.quotation_id
            LEFT JOIN (select * from product p where p.client_id = :clientId) p ON qi.product_id = p.id
            WHERE q.id = :quotationId 
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
        quotation.put("contactNumber", firstRow[10]);
        quotation.put("loadingCharge", firstRow[11]);

        // Process items
        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row[12]);
            item.put("quantity", row[13]);
            item.put("unitPrice", row[14]);
            item.put("discountPercentage", row[15]);
            item.put("discountAmount", row[16]);
            item.put("taxPercentage", row[17]);
            item.put("taxAmount", row[18]);
            item.put("finalPrice", row[19]);
            item.put("productId", row[20]);
            item.put("productName", row[21]);
            item.put("productType", row[22]);
            item.put("calculationType", row[23]);
            item.put("discountPrice", row[24]);
            item.put("measurement", row[25]);
            items.add(item);
        }

        // Get calculations for each item
        for (Map<String, Object> item : items) {
            Long itemId = (Long) item.get("id");
            List<Map<String, Object>> calculations = getCalculationsForItem(itemId);
            item.put("calculations", calculations);
        }

        quotation.put("items", items);
        return quotation;
    }

    private List<Map<String, Object>> getCalculationsForItem(Long itemId) {
        String sql = """
            SELECT 
                qic.feet, qic.inch, qic.mm, qic.nos, 
                qic.running_feet, qic.sq_feet, qic.weight
            FROM quotation_item_calculations qic
            WHERE qic.quotation_item_id = :itemId
        """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("itemId", itemId);
        
        List<Object[]> results = query.getResultList();
        return results.stream().map(row -> {
            Map<String, Object> calc = new HashMap<>();
            int i = 0;
            calc.put("feet", row[i++]);
            calc.put("inch", row[i++]);
            calc.put("mm", row[i++]);
            calc.put("nos", row[i++]);
            calc.put("runningFeet", row[i++]);
            calc.put("sqFeet", row[i++]);
            calc.put("weight", row[i++]);
            return calc;
        }).collect(Collectors.toList());
    }
} 