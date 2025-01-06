package com.inventory.repository;

import com.inventory.entity.Purchase;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    @Query("SELECT p FROM Purchase p WHERE p.client.id = :clientId")
    List<Purchase> findByClientId(@Param("clientId") Long clientId);
    
    @Query("SELECT p FROM Purchase p WHERE p.id = :id AND p.client.id = :clientId")
    Optional<Purchase> findByIdAndClientId(@Param("id") Long id, @Param("clientId") Long clientId);
}