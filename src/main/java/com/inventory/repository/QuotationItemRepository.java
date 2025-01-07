package com.inventory.repository;

import com.inventory.entity.Quotation;
import com.inventory.entity.QuotationItem;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuotationItemRepository extends JpaRepository<QuotationItem, Long> {

    @Modifying
    @Query("DELETE FROM QuotationItem qi WHERE qi.quotation.id = :quotationId")
    void deleteByQuotationId(Long quotationId);
} 