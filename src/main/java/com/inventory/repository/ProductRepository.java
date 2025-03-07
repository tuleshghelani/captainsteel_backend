package com.inventory.repository;

import com.inventory.entity.Product;
import com.inventory.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByStatus(String status);
    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByCategory(Category category);
    Optional<Product> findByName(String name);
    Optional<Product> findByNameAndIdNotIn(String name, List<Long> ids);
    Optional<Product> findByNameAndIdNotInAndClient_Id(String name, List<Long> ids, Long clinetId);
}