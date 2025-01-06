package com.inventory.repository;

import com.inventory.entity.UserMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserMaster, Long> {
    Optional<UserMaster> findByEmail(String email);
    boolean existsByEmail(String email);
}