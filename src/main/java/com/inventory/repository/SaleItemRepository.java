package com.inventory.repository;

import com.inventory.entity.PurchaseItem;
import com.inventory.entity.SaleItem;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    @QueryHints(@QueryHint(name = org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE, value = "100"))
    List<SaleItem> findBySaleId(@Param("saleId") Long saleId);

    @Modifying
    @Query("DELETE FROM SaleItem si WHERE si.sale.id = :saleId")
    void deleteBySaleId(@Param("saleId") Long saleId);
}