package com.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventory.entity.QuotationItemCalculation;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuotationItemCalculationRepository extends JpaRepository<QuotationItemCalculation, Long> {
    void deleteByQuotationItemId(Long quotationItemId);

    @Modifying
    @Query("DELETE FROM QuotationItemCalculation qic WHERE qic.quotation.id = :quotationId")
    void deleteByQuotationId(Long quotationId);
    List<QuotationItemCalculation> findByQuotationId(Long quotationId);
}