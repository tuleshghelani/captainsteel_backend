package com.inventory.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.inventory.entity.Quotation;
import org.springframework.stereotype.Repository;

import com.inventory.dto.QuotationDto;
import com.inventory.exception.ValidationException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.time.LocalDate;

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
                qi.discount_price, p.measurement, p.poly_carbonate_type, qi.item_remarks,
                qi.is_production, qi.quotation_item_status, qi.accessories_size, qi.weight,
                qi.nos, q.quotation_discount, q.quotation_discount_amount
            FROM (select * from quotation q where q.client_id = :clientId and q.id = :quotationId) q
            LEFT JOIN (select * from customer c where c.client_id = :clientId) c ON q.customer_id = c.id
            LEFT JOIN (select * from quotation_items qi where qi.client_id = :clientId) qi ON q.id = qi.quotation_id
            LEFT JOIN (select * from product p where p.client_id = :clientId) p ON qi.product_id = p.id
            WHERE q.id = :quotationId 
            order by qi.id
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
        quotation.put("quotationDiscount", firstRow[33]);
        quotation.put("quotationDiscountAmount", firstRow[34]);

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
            item.put("polyCarbonateType", row[26]);
            item.put("itemRemarks", row[27]);
            item.put("isProduction", row[28]);
            item.put("quotationItemStatus", row[29]);
            item.put("accessoriesSize", row[30]);
            item.put("weight", row[31]);
            item.put("nos", row[32]);
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

    public List<Quotation> findByClientIdAndDateRange(Long clientId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT * FROM quotation q 
            WHERE q.client_id = :clientId 
            AND q.quote_date BETWEEN :startDate AND :endDate
        """;

        Query query = entityManager.createNativeQuery(sql, Quotation.class);
        query.setParameter("clientId", clientId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        return query.getResultList();
    }
    
    /**
     * Search quotation items with quotation details
     * @param searchParams The search parameters
     * @return Map containing the search results and pagination info
     */
    public Map<String, Object> searchQuotationItemsWithDetails(QuotationDto searchParams) {
        Map<String, Object> params = new HashMap<>();
        params.put("clientId", searchParams.getClientId());
        
        // Build the main query
        StringBuilder sql = new StringBuilder("""
            SELECT 
                qi.id as item_id, qi.quantity, qi.unit_price, qi.weight,
                qi.discount_percentage, qi.discount_amount, qi.discount_price,
                qi.tax_percentage, qi.tax_amount, qi.final_price,
                qi.loading_charge, qi.accessories_size, qi.nos,
                qi.calculation_type, qi.calculation_base, qi.item_remarks,
                qi.is_production, qi.quotation_item_status, qi.quotation_discount_amount,
                p.id as product_id, p.name as product_name, p.type as product_type,
                q.id as quotation_id, q.quote_number, q.quote_date,
                q.valid_until, q.status as quotation_status, q.customer_name,
                q.contact_number, q.address, q.total_amount as quotation_total_amount,
                q.quotation_discount, c.id as customer_id
            FROM (select * from quotation_items qi where qi.client_id = :clientId """);

        params.put("clientId", searchParams.getClientId());
            if (searchParams.getQuotationItemStatuses() != null && !searchParams.getQuotationItemStatuses().isEmpty()) {
                sql.append(" AND qi.quotation_item_status IN (:quotationItemStatuses) ");
                params.put("quotationItemStatuses", searchParams.getQuotationItemStatuses());
            }

            if (searchParams.getQuotationId() != null) {
                sql.append(" AND qi.quotation_id = :quotationId ");
                params.put("quotationId", searchParams.getQuotationId());
            }

            if (searchParams.getProductId() != null) {
                sql.append(" AND qi.product_id = :productId ");
                params.put("productId", searchParams.getProductId());
            }

            if (searchParams.getIsProduction() != null) {
                sql.append(" AND qi.is_production = :isProduction ");
                params.put("isProduction", searchParams.getIsProduction());
            }
    sql.append(" ) qi " +
           " JOIN (select * from quotation q WHERE q.client_id = :clientId ");

        params.put("clientId", searchParams.getClientId());
    // Quotation filters
    if (searchParams.getQuotationStatuses() != null && !searchParams.getQuotationStatuses().isEmpty()) {
        sql.append(" AND q.status IN (:quotationStatuses)");
        params.put("quotationStatuses", searchParams.getQuotationStatuses());
    }

    if (searchParams.getCustomerId() != null) {
        sql.append(" AND q.customer_id = :customerId");
        params.put("customerId", searchParams.getCustomerId());
    }

    if (searchParams.getStartDate() != null) {
        sql.append(" AND q.quote_date >= :startDate");
        params.put("startDate", searchParams.getStartDate());
    }

    if (searchParams.getEndDate() != null) {
        sql.append(" AND q.quote_date <= :endDate");
        params.put("endDate", searchParams.getEndDate());
    }

    // General search
    if (searchParams.getSearch() != null && !searchParams.getSearch().trim().isEmpty()) {
        sql.append(" AND (q.quote_number LIKE :search OR p.name LIKE :search OR q.customer_name LIKE :search)");
        params.put("search", "%" + searchParams.getSearch().trim() + "%");
    }
    sql.append(" ) q ON qi.quotation_id = q.id " +
           " JOIN (select * from product p where p.client_id = :clientId) p ON qi.product_id = p.id " +
           " LEFT JOIN customer c ON q.customer_id = c.id " +
           " WHERE qi.client_id = :clientId ");
        
        // Build conditions
//        buildQuotationItemSearchConditions(conditions, params, searchParams);
        
        // Count query

        params.put("clientId", searchParams.getClientId());
        String countSql = "SELECT COUNT(*) from (" + sql.toString() +") t1 ";
        Query countQuery = entityManager.createNativeQuery(countSql);
        setQueryParameters(countQuery, params, null);
        Long totalRecords = ((Number) countQuery.getSingleResult()).longValue();
        
        // Main query with pagination
//        sql.append(conditions);
        sql.append(" ORDER BY qi.").append(searchParams.getSortBy()).append(" ")
           .append(searchParams.getSortDir());
        sql.append(" LIMIT :pageSize OFFSET :offset");
        
        Query query = entityManager.createNativeQuery(sql.toString());
        setQueryParameters(query, params, searchParams);
        
        List<Object[]> results = query.getResultList();
        return transformQuotationItemResults(results, totalRecords, searchParams);
    }
    
    private void buildQuotationItemSearchConditions(StringBuilder conditions, Map<String, Object> params, QuotationDto searchParams) {
        // Quotation item filters
        if (searchParams.getQuotationItemStatuses() != null && !searchParams.getQuotationItemStatuses().isEmpty()) {
            conditions.append(" AND qi.quotation_item_status IN :quotationItemStatuses ");
            params.put("quotationItemStatuses", searchParams.getQuotationItemStatuses());
        }
        
        if (searchParams.getQuotationId() != null) {
            conditions.append(" AND qi.quotation_id = :quotationId");
            params.put("quotationId", searchParams.getQuotationId());
        }
        
        if (searchParams.getProductId() != null) {
            conditions.append(" AND qi.product_id = :productId");
            params.put("productId", searchParams.getProductId());
        }
        
        if (searchParams.getIsProduction() != null) {
            conditions.append(" AND qi.is_production = :isProduction");
            params.put("isProduction", searchParams.getIsProduction());
        }

    }
    
    private Map<String, Object> transformQuotationItemResults(List<Object[]> results, Long totalRecords, QuotationDto searchParams) {
        List<Map<String, Object>> items = new ArrayList<>();
        
        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            int index = 0;
            
            // Quotation Item fields
            item.put("id", row[index++]);
            item.put("quantity", row[index++]);
            item.put("unitPrice", row[index++]);
            item.put("weight", row[index++]);
            item.put("discountPercentage", row[index++]);
            item.put("discountAmount", row[index++]);
            item.put("discountPrice", row[index++]);
            item.put("taxPercentage", row[index++]);
            item.put("taxAmount", row[index++]);
            item.put("finalPrice", row[index++]);
            item.put("loadingCharge", row[index++]);
            item.put("accessoriesSize", row[index++]);
            item.put("nos", row[index++]);
            item.put("calculationType", row[index++]);
            item.put("calculationBase", row[index++]);
            item.put("itemRemarks", row[index++]);
            item.put("isProduction", row[index++]);
            item.put("quotationItemStatus", row[index++]);
            item.put("quotationDiscountAmount", row[index++]);
            
            // Product fields
            item.put("productId", row[index++]);
            item.put("productName", row[index++]);
            item.put("productType", row[index++]);
            
            // Quotation fields
            item.put("quotationId", row[index++]);
            item.put("quoteNumber", row[index++]);
            item.put("quoteDate", row[index++]);
            item.put("validUntil", row[index++]);
            item.put("quotationStatus", row[index++]);
            item.put("customerName", row[index++]);
            item.put("contactNumber", row[index++]);
            item.put("address", row[index++]);
            item.put("quotationTotalAmount", row[index++]);
            item.put("quotationDiscount", row[index++]);
            
            // Customer fields
            item.put("customerId", row[index]);
            
            items.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", items);
        response.put("currentPage", searchParams.getCurrentPage());
        response.put("totalItems", totalRecords);
        response.put("totalPages", (int) Math.ceil((double) totalRecords / searchParams.getPerPageRecord()));

        return response;
    }
}