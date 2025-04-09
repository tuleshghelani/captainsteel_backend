package com.inventory.dao;

import com.inventory.dto.CustomerDto;
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
public class CustomerDao {
    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> searchCustomers(CustomerDto dto) {
        StringBuilder countSql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        countSql.append("SELECT COUNT(c.id) FROM customer c WHERE c.client_id = :clientId");
        params.put("clientId", dto.getClientId());

        if (StringUtils.hasText(dto.getSearch())) {
            countSql.append(" AND (LOWER(c.name) LIKE LOWER(:search) OR c.mobile LIKE :search OR LOWER(c.gst) LIKE LOWER(:search))");
            params.put("search", "%" + dto.getSearch().trim() + "%");
        }
        if(dto.getStartDate() != null){
            countSql.append(" AND c.next_action_date >= :startDate");
            params.put("startDate", dto.getStartDate());
        }
        if(dto.getEndDate() != null){
            countSql.append(" AND c.next_action_date <= :endDate");
            params.put("endDate", dto.getEndDate());
        }

        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT 
                c.id, c.name, c.gst, c.address, c.mobile,
                c.remaining_payment_amount, c.next_action_date,
                c.email, c.remarks, c.status, c.coating_unit_price,
                c.created_at, c.updated_at
            FROM customer c
            WHERE c.client_id = :clientId
        """);

        if (StringUtils.hasText(dto.getSearch())) {
            sql.append(" AND (LOWER(c.name) LIKE LOWER(:search))");
        }
        if(dto.getStartDate() != null){
            sql.append(" AND c.next_action_date >= :startDate");
            params.put("startDate", dto.getStartDate());
        }
        if(dto.getEndDate() != null){
            sql.append(" AND c.next_action_date <= :endDate");
            params.put("endDate", dto.getEndDate());
        }

        params.put("clientId", dto.getClientId());

        sql.append(" ORDER BY c.id DESC LIMIT :pageSize OFFSET :offset");

        Query query = entityManager.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);
        query.setParameter("pageSize", dto.getPerPageRecord());
        query.setParameter("offset", (long) dto.getCurrentPage() * dto.getPerPageRecord());

        List<Object[]> results = query.getResultList();
        return transformResults(results, totalRecords, dto.getPerPageRecord());
    }

    private Map<String, Object> transformResults(List<Object[]> results, long totalRecords, int pageSize) {
        List<Map<String, Object>> customers = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> customer = new HashMap<>();
            customer.put("id", row[0]);
            customer.put("name", row[1]);
            customer.put("gst", row[2]);
            customer.put("address", row[3]);
            customer.put("mobile", row[4]);
            customer.put("remainingPaymentAmount", row[5]);
            customer.put("nextActionDate", row[6]);
            customer.put("email", row[7]);
            customer.put("remarks", row[8]);
            customer.put("status", row[9]);
            customer.put("coatingUnitPrice", row[10]);
            customer.put("createdAt", row[11]);
            customer.put("updatedAt", row[12]);
            customers.add(customer);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", customers);
        response.put("totalElements", totalRecords);
        response.put("totalPages", (int) Math.ceil((double) totalRecords / pageSize));

        return response;
    }

    
    public List<Map<String, Object>> getCustomers(CustomerDto dto) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        sql.append("""
            SELECT 
                c.id,
                c.name,
                c.address,
                c.mobile
            FROM customer c
            WHERE c.status = 'A' AND c.client_id = :clientId
        """);
        params.put("clientId", dto.getClientId());

        if (StringUtils.hasText(dto.getSearch())) {
            sql.append(" AND LOWER(c.name) LIKE LOWER(:search)");
            params.put("search", "%" + dto.getSearch().trim() + "%");
        }

        sql.append(" ORDER BY c.name ASC");

        Query query = entityManager.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        List<Object[]> results = query.getResultList();
        return transformResults(results);
    }

    private List<Map<String, Object>> transformResults(List<Object[]> results) {
        List<Map<String, Object>> customers = new ArrayList<>();
        
        for (Object[] row : results) {
            Map<String, Object> customer = new HashMap<>();
            customer.put("id", row[0]);
            customer.put("name", row[1]);
            customer.put("address", row[2]);
            customer.put("mobile", row[3]);
            customers.add(customer);
        }
        
        return customers;
    }
} 