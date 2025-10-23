package com.inventory.repository;

import com.inventory.entity.Quotation;
import com.inventory.entity.QuotationItem;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationItemRepository extends JpaRepository<QuotationItem, Long> {

    @Modifying
    @Query("DELETE FROM QuotationItem qi WHERE qi.quotation.id = :quotationId")
    void deleteByQuotationId(Long quotationId);

    List<QuotationItem> findByQuotationId(Long quotationId);

    @Modifying
    @Query("UPDATE QuotationItem qi SET qi.quotationItemStatus = :status WHERE qi.id = :id")
    int updateQuotationItemStatusById(Long id, String status);
    
    // Search quotation items with quotation details
    @Query("SELECT qi FROM QuotationItem qi " +
           "JOIN FETCH qi.quotation q " +
           "JOIN FETCH qi.product p " +
           "WHERE (:clientId IS NULL OR qi.client.id = :clientId) " +
           "AND (:quotationItemStatus IS NULL OR qi.quotationItemStatus = :quotationItemStatus) " +
           "AND (:quotationId IS NULL OR qi.quotation.id = :quotationId) " +
           "AND (:productId IS NULL OR qi.product.id = :productId) " +
           "AND (:isProduction IS NULL OR qi.isProduction = :isProduction) " +
           "AND (:quotationStatus IS NULL OR q.status = :quotationStatus) " +
           "AND (:customerId IS NULL OR q.customer.id = :customerId) " +
           "AND (:quoteStartDate IS NULL OR q.quoteDate >= :quoteStartDate) " +
           "AND (:quoteEndDate IS NULL OR q.quoteDate <= :quoteEndDate) " +
           "AND (:search IS NULL OR (LOWER(q.quoteNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(q.customerName) LIKE LOWER(CONCAT('%', :search, '%'))))")
    List<QuotationItem> searchQuotationItemsWithDetails(
        @Param("clientId") Long clientId,
        @Param("quotationItemStatus") String quotationItemStatus,
        @Param("quotationId") Long quotationId,
        @Param("productId") Long productId,
        @Param("isProduction") Boolean isProduction,
        @Param("quotationStatus") String quotationStatus,
        @Param("customerId") Long customerId,
        @Param("quoteStartDate") java.time.LocalDate quoteStartDate,
        @Param("quoteEndDate") java.time.LocalDate quoteEndDate,
        @Param("search") String search
    );
    
    // Count quotation items with quotation details for pagination
    @Query("SELECT COUNT(qi) FROM QuotationItem qi " +
           "JOIN qi.quotation q " +
           "JOIN qi.product p " +
           "WHERE (:clientId IS NULL OR qi.client.id = :clientId) " +
           "AND (:quotationItemStatus IS NULL OR qi.quotationItemStatus = :quotationItemStatus) " +
           "AND (:quotationId IS NULL OR qi.quotation.id = :quotationId) " +
           "AND (:productId IS NULL OR qi.product.id = :productId) " +
           "AND (:isProduction IS NULL OR qi.isProduction = :isProduction) " +
           "AND (:quotationStatus IS NULL OR q.status = :quotationStatus) " +
           "AND (:customerId IS NULL OR q.customer.id = :customerId) " +
           "AND (:quoteStartDate IS NULL OR q.quoteDate >= :quoteStartDate) " +
           "AND (:quoteEndDate IS NULL OR q.quoteDate <= :quoteEndDate) " +
           "AND (:search IS NULL OR (LOWER(q.quoteNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(q.customerName) LIKE LOWER(CONCAT('%', :search, '%'))))")
    long countQuotationItemsWithDetails(
        @Param("clientId") Long clientId,
        @Param("quotationItemStatus") String quotationItemStatus,
        @Param("quotationId") Long quotationId,
        @Param("productId") Long productId,
        @Param("isProduction") Boolean isProduction,
        @Param("quotationStatus") String quotationStatus,
        @Param("customerId") Long customerId,
        @Param("quoteStartDate") java.time.LocalDate quoteStartDate,
        @Param("quoteEndDate") java.time.LocalDate quoteEndDate,
        @Param("search") String search
    );
}