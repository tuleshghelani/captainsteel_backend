package com.inventory.service;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.ProductDto;
import com.inventory.entity.Product;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.CategoryRepository;
import com.inventory.dao.ProductDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductDao productDao;
    private final UtilityService utilityService;

    @Transactional
    public ApiResponse<?> create(ProductDto dto) {
        validateProduct(dto);
        
        try {
            Optional<Product> productByName = productRepository.findByName(dto.getName().trim());
            if(!productByName.isEmpty()) {
                throw new ValidationException("Product name already exists");
            }
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();

            Product product = new Product();
            product.setName(dto.getName().trim());
            product.setCategory(categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ValidationException("Category not found")));
            product.setDescription(dto.getDescription());
            product.setPurchaseAmount(dto.getPurchaseAmount() != null ? dto.getPurchaseAmount() : BigDecimal.valueOf(0));
            product.setSaleAmount(dto.getSaleAmount() != null ? dto.getSaleAmount() : BigDecimal.valueOf(0));
            product.setMinimumStock(dto.getMinimumStock());
            product.setStatus(dto.getStatus().trim());
            product.setClient(currentUser.getClient());
            product.setCreatedBy(currentUser);

            productRepository.save(product);
            return ApiResponse.success("Product created successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to create product");
        }
    }

    @Transactional
    public ApiResponse<?> update(Long id, ProductDto dto) {
        validateProduct(dto);
        
        try {
            Product product = productRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Product not found"));

            Optional<Product> productByName = productRepository.findByNameAndIdNotIn(dto.getName().trim(), Collections.singletonList(product.getId()));
            if(!productByName.isEmpty()) {
                throw new ValidationException("Product name already exists");
            }
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(product.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to update this product");
            }
            
            product.setName(dto.getName().trim());
            product.setCategory(categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ValidationException("Category not found")));
            product.setDescription(dto.getDescription());
            product.setPurchaseAmount(dto.getPurchaseAmount() != null ? dto.getPurchaseAmount() : BigDecimal.valueOf(0));
            product.setSaleAmount(dto.getSaleAmount() != null ? dto.getSaleAmount() : BigDecimal.valueOf(0));
            product.setMinimumStock(dto.getMinimumStock());
            product.setStatus(dto.getStatus().trim());
            product.setClient(currentUser.getClient());

            productRepository.save(product);
            return ApiResponse.success("Product updated successfully");
        } catch (ValidationException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to update product");
        }
    }

    public ApiResponse<List<Map<String, Object>>> getProducts(ProductDto productDto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            productDto.setClientId(currentUser.getClient().getId());
            List<Map<String, Object>> products = productDao.getProducts(productDto);
            return ApiResponse.success("Products retrieved successfully", products);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to retrieve products");
        }
    }
    
    public ApiResponse<Map<String, Object>> searchProducts(ProductDto productDto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            productDto.setClientId(currentUser.getClient().getId());
            Map<String, Object> result = productDao.searchProducts(productDto);
            return ApiResponse.success("Products retrieved successfully", result);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to retrieve products");
        }
    }

    @Transactional
    public ApiResponse<?> delete(Long id) {
        try {
            Product product = productRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Product not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(product.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to delete this product");
            }
            productRepository.delete(product);
            return ApiResponse.success("Product deleted successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to delete product");
        }
    }

    private void validateProduct(ProductDto dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new ValidationException("Product name is required");
        }
        if (dto.getCategoryId() == null) {
            throw new ValidationException("Category is required");
        }
        if (!StringUtils.hasText(dto.getStatus())) {
            throw new ValidationException("Product status is required");
        }
        if (dto.getStatus().trim().length() != 1 || !dto.getStatus().trim().matches("[AI]")) {
            throw new ValidationException("Product status must be either 'A' (Active) or 'I' (Inactive)");
        }
        if (dto.getMinimumStock() == null || dto.getMinimumStock().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Minimum stock must be a non-negative number");
        }
    }

    private ProductDto mapToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setCategoryId(product.getCategory().getId());
        dto.setDescription(product.getDescription());
        dto.setMinimumStock(product.getMinimumStock());
        dto.setStatus(product.getStatus());
        dto.setRemainingQuantity(product.getRemainingQuantity());
        dto.setClientId(product.getClient().getId());
        return dto;
    }
}
