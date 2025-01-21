package com.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventory.entity.QuotationItemCalculation;

public interface QuotationItemCalculationRepository extends JpaRepository<QuotationItemCalculation, Long> {
    void deleteByQuotationItemId(Long quotationItemId);
} 