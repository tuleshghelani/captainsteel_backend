package com.inventory.repository;

import com.inventory.entity.Category;
import com.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByStatus(String status);
    Optional<Category> findByName(String name);
    Optional<Category> findByNameAndIdNotIn(String name, List<Long> ids);
    Optional<Category> findByNameAndIdNotInAndClient_Id(String name, List<Long> ids, Long clinetId);
}