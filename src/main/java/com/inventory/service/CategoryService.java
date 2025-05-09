package com.inventory.service;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.CategoryDto;
import com.inventory.entity.Category;
import com.inventory.entity.Product;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.dao.CategoryDao;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.OffsetDateTime;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryDao categoryDao;
    private final UtilityService utilityService;

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> create(CategoryDto dto) {
        validateCategory(dto);
        
        try {
            Optional<Category> categoryByName = categoryRepository.findByName(dto.getName().trim());
            if(!categoryByName.isEmpty()) {
                throw new ValidationException("Category name already exist");
            }
            
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            
            Category category = new Category();
            category.setName(dto.getName().trim());
            category.setStatus(dto.getStatus().trim());
            category.setCreatedBy(currentUser);
            category.setClient(currentUser.getClient());
            
            categoryRepository.save(category);
            return ApiResponse.success("Category created successfully");
        } catch (ValidationException e) {
            e.printStackTrace();
            throw new ValidationException(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to create category");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> update(Long id, CategoryDto dto) {
        validateCategory(dto);
        
        try {
            Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Category not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if (!Objects.equals(category.getClient().getId(), currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to update this category");
            }

            Optional<Category> categoryByName = categoryRepository.findByNameAndIdNotInAndClient_Id(
                    dto.getName().trim(), Collections.singletonList(category.getId()), currentUser.getClient().getId());
            if(!categoryByName.isEmpty()) {
                throw new ValidationException("Category name already exist");
            }

            
            category.setName(dto.getName().trim());
            category.setStatus(dto.getStatus().trim());
            category.setUpdatedAt(OffsetDateTime.now());
            
            categoryRepository.save(category);
            return ApiResponse.success("Category updated successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update category");
        }
    }

    @Transactional
    public ApiResponse<?> delete(Long id) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Category not found"));
            if(category.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to delete this category");
            }

            List<Product> existingProducts = productRepository.findByCategory(category);
            if (!existingProducts.isEmpty()) {
                throw new ValidationException("Cannot delete category. There are products associated with this category");
            }

            categoryRepository.delete(category);
            return ApiResponse.success("Category deleted successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to delete category");
        }
    }

    public ApiResponse<List<Map<String, Object>>> getCategories(CategoryDto categoryDto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            categoryDto.setClientId(currentUser.getClient().getId());
            List<Map<String, Object>> categories = categoryDao.getCategories(categoryDto);
            return ApiResponse.success("Categories retrieved successfully", categories);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to retrieve categories");
        }
    }

    public ApiResponse<Map<String, Object>> searchCategories(CategoryDto categoryDto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            categoryDto.setClientId(currentUser.getClient().getId());
            Map<String, Object> result = categoryDao.searchCategories(categoryDto);
            return ApiResponse.success("Categories retrieved successfully", result);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to retrieve categories");
        }
    }

    private void validateCategory(CategoryDto dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new ValidationException("Category name is required");
        }
        if (!StringUtils.hasText(dto.getStatus())) {
            throw new ValidationException("Category status is required");
        }
        if (dto.getStatus().trim().length() != 1 || !dto.getStatus().trim().matches("[AI]")) {
            throw new ValidationException("Category status must be either 'A' (Active) or 'I' (Inactive)");
        }
    }

    private CategoryDto mapToDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setStatus(category.getStatus());
        dto.setRemainingQuantity(category.getRemainingQuantity());
        dto.setClientId(category.getClient().getId());
        return dto;
    }
}
