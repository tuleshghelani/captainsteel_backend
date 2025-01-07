package com.inventory.repository;

import com.inventory.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import jakarta.persistence.QueryHint;
import java.util.Optional;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<Quotation> findByQuoteNumberAndClientId(String quoteNumber, Long clientId);
} 