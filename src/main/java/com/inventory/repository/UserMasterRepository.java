package com.inventory.repository;

import com.inventory.entity.UserMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserMasterRepository extends JpaRepository<UserMaster, Long> {
    Optional<UserMaster> findByRefreshToken(String refreshToken);
} 