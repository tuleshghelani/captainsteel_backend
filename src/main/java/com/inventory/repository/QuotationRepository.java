package com.inventory.repository;

import com.inventory.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<Quotation> findByQuoteNumberAndClientId(String quoteNumber, Long clientId);

    @Query("SELECT q FROM Quotation q WHERE q.client.id = :clientId AND q.quoteDate BETWEEN :startDate AND :endDate")
    List<Quotation> findByClientIdAndDateRange(
        @org.springframework.data.repository.query.Param("clientId") Long clientId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
} 