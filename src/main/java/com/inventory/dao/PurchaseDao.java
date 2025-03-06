package com.inventory.dao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hibernate.jpa.QueryHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.inventory.dto.PurchaseDto;
import com.inventory.entity.Purchase;
import com.inventory.entity.PurchaseItem;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class PurchaseDao {
    @PersistenceContext
    private EntityManager entityManager;

    public Page<Map<String, Object>> searchPurchases(PurchaseDto dto) {
        try {
            StringBuilder countQuery = new StringBuilder();
            StringBuilder actualQuery = new StringBuilder();
            StringBuilder nativeQuery = new StringBuilder();
            Map<String, Object> params = new HashMap<>();

            actualQuery.append("""
                SELECT 
                    p.id, p.total_purchase_amount, 
                    p.purchase_date, p.invoice_number, c.name as customer_name
                """);

            countQuery.append("SELECT COUNT(*) ");

            nativeQuery.append("""
                FROM (select * from purchase p where p.client_id = :clientId) p 
                LEFT JOIN (select * from customer c where c.client_id = :clientId) c ON p.customer_id = c.id
                WHERE 1=1
                """);
            params.put("clientId", dto.getClientId());

            appendSearchConditions(nativeQuery, params, dto);

            countQuery.append(nativeQuery);
            nativeQuery.append(" ORDER BY p.id DESC LIMIT :perPageRecord OFFSET :offset");
            actualQuery.append(nativeQuery);

            Pageable pageable = PageRequest.of(dto.getCurrentPage(), dto.getPerPageRecord());
            Query countQueryObj = entityManager.createNativeQuery(countQuery.toString());
            Query query = entityManager.createNativeQuery(actualQuery.toString());

            setQueryParameters(query, countQueryObj, params, dto);

            Long totalCount = ((Number) countQueryObj.getSingleResult()).longValue();
            List<Object[]> results = query.getResultList();
            List<Map<String, Object>> purchases = transformResults(results);

            return new PageImpl<>(purchases, pageable, totalCount);
        } catch (Exception e) {
            e.printStackTrace();
            return new PageImpl<>(new ArrayList<>(), 
                PageRequest.of(dto.getCurrentPage(), dto.getPerPageRecord()), 0L);
        }
    }

    private void appendSearchConditions(StringBuilder sql, Map<String, Object> params, PurchaseDto dto) {
        if (!Objects.isNull(dto.getSearch()) && dto.getSearch().trim().length() > 0) {
            sql.append("""
                AND (LOWER(p.invoice_number) LIKE :search)
                """);
            params.put("search", "%" + dto.getSearch().toLowerCase().trim() + "%");
        }
        if(!Objects.isNull(dto.getStartDate())) {
            sql.append("""
                AND (p.purchase_date >= :startDate)
                """);
            params.put("startDate", dto.getStartDate());
        }
        if(!Objects.isNull(dto.getEndDate())) {
            sql.append("""
                AND (p.purchase_date <= :endDate)
                """);
            params.put("endDate", dto.getEndDate());
        }
        if(!Objects.isNull(dto.getCoilNumber()) && !dto.getCoilNumber().isEmpty()) {
            sql.append("""
                AND p.coil_numbers @> CAST(:coilNumber AS jsonb)
                """);
            params.put("coilNumber", "[\"" + dto.getCoilNumber().trim().toLowerCase() + "\"]");
        }
        if(!Objects.isNull(dto.getCustomerId())) {
            sql.append("""
                AND p.customer_id = :customerId
                """);
            params.put("customerId", dto.getCustomerId());
        }

        // if(!Objects.isNull(dto.getcoilNumber()) && !dto.getcoilNumber().isEmpty()) {
        //     sql.append("""
        //         AND EXISTS (
        //             SELECT FROM jsonb_array_elements_text(p.coil_numbers)
        //             WHERE value LIKE :coilNumber
        //         )
        //         """);
        //     params.put("coilNumber", "%" + dto.getcoilNumber() + "%");
        // }
    }

    private void setQueryParameters(Query query, Query countQuery, Map<String, Object> params, PurchaseDto dto) {
        params.forEach((key, value) -> {
            query.setParameter(key, value);
            countQuery.setParameter(key, value);
        });

        query.setParameter("perPageRecord", dto.getPerPageRecord());
        query.setParameter("offset", (long) dto.getCurrentPage() * dto.getPerPageRecord());
    }

    private List<Map<String, Object>> transformResults(List<Object[]> results) {
        List<Map<String, Object>> purchases = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> purchase = new HashMap<>();
            int i = 0;
            purchase.put("id", row[i++]);
            purchase.put("totalPurchaseAmount", row[i++]);
            purchase.put("purchaseDate", row[i++]);
            purchase.put("invoiceNumber", row[i++]);
            purchase.put("customerName", row[i++]);
            purchases.add(purchase);
        }
        return purchases;
    }

    
    public List<Purchase> findPurchasesByDateRange(LocalDateTime startDate, LocalDateTime endDate, 
            int limit, int offset, Long clientId) {
        String jpql = """
            SELECT DISTINCT p FROM Purchase p 
            LEFT JOIN FETCH p.customer 
            LEFT JOIN FETCH p.purchaseItems i 
            LEFT JOIN FETCH i.product 
            WHERE p.client.id = :clientId 
            AND p.purchaseDate BETWEEN :startDate AND :endDate 
            ORDER BY p.id
            """;
            
        return entityManager.createQuery(jpql, Purchase.class)
            .setParameter("clientId", clientId)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .setMaxResults(limit)
            .setFirstResult(offset)
            .setHint(QueryHints.HINT_FETCH_SIZE, 100)
            .getResultList();
    }
    
    public List<PurchaseItem> findPurchaseItemsByPurchaseId(Long purchaseId, Long clientId) {
        String jpql = """
            SELECT pi FROM PurchaseItem pi 
            JOIN FETCH pi.product p 
            WHERE pi.purchase.id = :purchaseId 
            AND pi.client.id = :clientId
            """;
            
        return entityManager.createQuery(jpql, PurchaseItem.class)
            .setParameter("purchaseId", purchaseId)
            .setParameter("clientId", clientId)
            .setHint(QueryHints.HINT_FETCH_SIZE, 100)
            .getResultList();
    }

    public Map<String, Object> getPurchaseDetail(Long purchaseId, Long clientId) {
        String sql = """
            SELECT 
                p.id, p.invoice_number, p.purchase_date, p.total_purchase_amount,
                p.created_at, p.updated_at, p.customer_id, p.created_by,
                pi.id as item_id, pi.quantity, pi.unit_price, pi.discount_percentage,
                pi.discount_amount, pi.final_price, 
                pi.product_id, pi.coil_number, pi.remarks
            FROM (SELECT * FROM purchase WHERE id = :purchaseId AND client_id = :clientId) p
            LEFT JOIN (SELECT * FROM purchase_items WHERE purchase_id = :purchaseId) pi ON p.id = pi.purchase_id
            WHERE p.id = :purchaseId
        """;

        Query query = entityManager.createNativeQuery(sql)
            .setParameter("purchaseId", purchaseId)
            .setParameter("clientId", clientId);

        List<Object[]> results = query.getResultList();
        return transformToDetailResponse(results);
    }

    private Map<String, Object> transformToDetailResponse(List<Object[]> results) {
        if (results.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> response = new HashMap<>();
        Object[] firstRow = results.get(0);

        // Set purchase details with null checks
        response.put("id", firstRow[0]);
        response.put("invoiceNumber", firstRow[1] != null ? firstRow[1] : "");
        response.put("purchaseDate", firstRow[2] != null ? firstRow[2] : "");
        response.put("totalAmount", firstRow[3] != null ? firstRow[3] : BigDecimal.ZERO);
        response.put("createdAt", firstRow[4] != null ? firstRow[4] : "");
        response.put("updatedAt", firstRow[5] != null ? firstRow[5] : "");
        response.put("customerId", firstRow[6]);
        response.put("createdBy", firstRow[7]);

        // Set items
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] row : results) {
            if (row[8] != null) { // if item exists
                items.add(Map.of(
                    "id", row[8],
                    "quantity", row[9] != null ? row[9] : 0,
                    "unitPrice", row[10] != null ? row[10] : BigDecimal.ZERO,
                    "discountPercentage", row[11] != null ? row[11] : 0,
                    "discountAmount", row[12] != null ? row[12] : BigDecimal.ZERO,
                    "finalPrice", row[13] != null ? row[13] : BigDecimal.ZERO,
                    "productId", row[14],
                    "coilNumber", row[15] != null ? row[15] : "",
                    "remarks", row[16] != null ? row[16] : ""
                ));
            }
        }
        response.put("items", items);

        return response;
    }
}