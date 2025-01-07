package com.inventory.repository;

import com.inventory.entity.Client;
import com.inventory.entity.UserMaster;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Client c WHERE c.id = :id")
    Optional<Client> findByIdWithPessimisticLock(@Param("id") Long id);
}