package com.inventory.repository;

import com.inventory.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByMobile(String mobile);
    Optional<Customer> findByMobileAndIdNot(String mobile, Long id);
    List<Customer> findByStatus(String status);
} 