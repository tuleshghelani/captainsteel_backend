package com.inventory.dao;

import com.inventory.dto.PurchaseDto;
import com.inventory.dto.SaleDto;
import com.inventory.entity.Sale;
import com.inventory.entity.PurchaseItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.hibernate.jpa.QueryHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public class SaleDao {
    @PersistenceContext
    private EntityManager entityManager;

    public Page<Map<String, Object>> searchSales(SaleDto dto) {
        try {
            StringBuilder countQuery = new StringBuilder();
            StringBuilder actualQuery = new StringBuilder();
            StringBuilder nativeQuery = new StringBuilder();
            Map<String, Object> params = new HashMap<>();

            actualQuery.append("""
                SELECT 
                    s.id, s.total_sale_amount, 
                    s.sale_date, s.invoice_number, c.name as customer_name, s.is_black
                """);

            countQuery.append("SELECT COUNT(*) ");

            nativeQuery.append("""
                FROM (select * from sale s where s.client_id = :clientId) s 
                LEFT JOIN (select * from customer c where c.client_id = :clientId) c ON s.customer_id = c.id
                WHERE 1=1
                """);
            params.put("clientId", dto.getClientId());

            appendSearchConditions(nativeQuery, params, dto);

            countQuery.append(nativeQuery);
            nativeQuery.append(" ORDER BY s.id DESC LIMIT :perPageRecord OFFSET :offset");
            actualQuery.append(nativeQuery);

            Pageable pageable = PageRequest.of(dto.getCurrentPage(), dto.getPerPageRecord());
            Query countQueryObj = entityManager.createNativeQuery(countQuery.toString());
            Query query = entityManager.createNativeQuery(actualQuery.toString());

            setQueryParameters(query, countQueryObj, params, dto);

            Long totalCount = ((Number) countQueryObj.getSingleResult()).longValue();
            List<Object[]> results = query.getResultList();
            List<Map<String, Object>> sales = transformResults(results);

            return new PageImpl<>(sales, pageable, totalCount);
        } catch (Exception e) {
            e.printStackTrace();
            return new PageImpl<>(new ArrayList<>(), 
                PageRequest.of(dto.getCurrentPage(), dto.getPerPageRecord()), 0L);
        }
    }

    private void appendSearchConditions(StringBuilder sql, Map<String, Object> params, SaleDto
            dto) {
        if (!Objects.isNull(dto.getSearch()) && dto.getSearch().trim().length() > 0) {
            sql.append("""
                AND (LOWER(s.invoice_number) LIKE :search)
                """);
            params.put("search", "%" + dto.getSearch().toLowerCase().trim() + "%");
        }
        if(!Objects.isNull(dto.getStartDate())) {
            sql.append("""
                AND (s.sale_date >= :startDate)
                """);
            params.put("startDate", dto.getStartDate());
        }
        if(!Objects.isNull(dto.getEndDate())) {
            sql.append("""
                AND (s.sale_date <= :endDate)
                """);
            params.put("endDate", dto.getEndDate());
        }
        if(!Objects.isNull(dto.getCoilNumber()) && !dto.getCoilNumber().isEmpty()) {
            sql.append("""
                AND s.coil_numbers @> CAST(:coilNumber AS jsonb)
                """);
            params.put("coilNumber", "[\"" + dto.getCoilNumber().trim().toLowerCase() + "\"]");
        }
        if(!Objects.isNull(dto.getCustomerId())) {
            sql.append("""
                AND s.customer_id = :customerId
                """);
            params.put("customerId", dto.getCustomerId());
        }

        // if(!Objects.isNull(dto.getcoilNumber()) && !dto.getcoilNumber().isEmpty()) {
        //     sql.append("""
        //         AND EXISTS (
        //             SELECT FROM jsonb_array_elements_text(s.coil_numbers)
        //             WHERE value LIKE :coilNumber
        //         )
        //         """);
        //     params.put("coilNumber", "%" + dto.getcoilNumber() + "%");
        // }
    }

    private void setQueryParameters(Query query, Query countQuery, Map<String, Object> params, SaleDto dto) {
        params.forEach((key, value) -> {
            query.setParameter(key, value);
            countQuery.setParameter(key, value);
        });

        query.setParameter("perPageRecord", dto.getPerPageRecord());
        query.setParameter("offset", (long) dto.getCurrentPage() * dto.getPerPageRecord());
    }

    private List<Map<String, Object>> transformResults(List<Object[]> results) {
        List<Map<String, Object>> sales = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> sale = new HashMap<>();
            int i = 0;
            sale.put("id", row[i++]);
            sale.put("totalSaleAmount", row[i++]);
            sale.put("saleDate", row[i++]);
            sale.put("invoiceNumber", row[i++]);
            sale.put("customerName", row[i++]);
            sale.put("is_black", row[i++]);
            sales.add(sale);
        }
        return sales;
    }

    public Map<String, Object> getSaleDetail(Long saleId, Long clientId) {
        String sql = """
            SELECT 
                s.id, s.invoice_number, s.sale_date, s.total_sale_amount,
                s.created_at, s.updated_at, s.customer_id, s.created_by, s.is_black,
                si.id as item_id, si.quantity, si.unit_price, si.discount_percentage,
                si.discount_amount, si.final_price, 
                si.product_id, si.coil_number, si.remarks
            FROM (SELECT * FROM sale WHERE id = :saleId AND client_id = :clientId) s
            LEFT JOIN (SELECT * FROM sale_items si WHERE sale_id = :saleId) si ON s.id = si.sale_id
            WHERE s.id = :saleId
        """;

        Query query = entityManager.createNativeQuery(sql)
            .setParameter("saleId", saleId)
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

        // Set sale details with null checks
        response.put("id", firstRow[0]);
        response.put("invoiceNumber", firstRow[1] != null ? firstRow[1] : "");
        response.put("saleDate", firstRow[2] != null ? firstRow[2] : "");
        response.put("totalAmount", firstRow[3] != null ? firstRow[3] : BigDecimal.ZERO);
        response.put("createdAt", firstRow[4] != null ? firstRow[4] : "");
        response.put("updatedAt", firstRow[5] != null ? firstRow[5] : "");
        response.put("customerId", firstRow[6]);
        response.put("createdBy", firstRow[7]);
        response.put("isBlack", firstRow[8]);

        // Set items
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] row : results) {
            if (row[9] != null) { // if item exists
                items.add(Map.of(
                    "id", row[9],
                    "quantity", row[10] != null ? row[10] : 0,
                    "unitPrice", row[11] != null ? row[11] : BigDecimal.ZERO,
                    "discountPercentage", row[12] != null ? row[12] : 0,
                    "discountAmount", row[13] != null ? row[13] : BigDecimal.ZERO,
                    "finalPrice", row[14] != null ? row[14] : BigDecimal.ZERO,
                    "productId", row[15],
                    "coilNumber", row[16] != null ? row[16] : "",
                    "remarks", row[17] != null ? row[17] : ""
                ));
            }
        }
        response.put("items", items);

        return response;
    }

    public List<Sale> findByClientIdAndDateRange(Long clientId, LocalDateTime startDate, LocalDateTime endDate) {
        String sql = """
            SELECT * FROM sale s 
            WHERE s.client_id = :clientId 
            AND s.sale_date BETWEEN :startDate AND :endDate
        """;

        Query query = entityManager.createNativeQuery(sql, Sale.class);
        query.setParameter("clientId", clientId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        return query.getResultList();
    }
}