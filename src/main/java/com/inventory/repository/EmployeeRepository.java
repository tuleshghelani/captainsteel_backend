package com.inventory.repository;

import com.inventory.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByMobileNumber(String mobileNumber);
    Optional<Employee> findByMobileNumberAndIdNot(String mobileNumber, Long id);
    List<Employee> findByStatus(String status);
} 