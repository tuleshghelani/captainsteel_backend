package com.inventory.repository;

import com.inventory.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByStatus(String status);
    Optional<Category> findByName(String name);
    Optional<Category> findByNameAndIdNotIn(String name, List<Long> ids);
}