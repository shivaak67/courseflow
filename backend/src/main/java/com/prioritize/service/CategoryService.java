package com.prioritize.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.CategoryRequest;
import com.prioritize.dto.CategoryResponse;
import com.prioritize.exception.ApiException;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.CategoryMapper;
import com.prioritize.model.Category;
import com.prioritize.repository.CategoryRepository;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(UUID userId) {
        return categoryRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(UUID userId, UUID categoryId) {
        return categoryMapper.toResponse(requireOwned(userId, categoryId));
    }

    public CategoryResponse create(UUID userId, CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Category name already exists");
        }
        Category category = new Category();
        category.setUserId(userId);
        categoryMapper.applyCreate(category, request);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    public CategoryResponse update(UUID userId, UUID categoryId, CategoryRequest request) {
        Category category = requireOwned(userId, categoryId);
        String name = request.name().trim();
        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, name, categoryId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Category name already exists");
        }
        categoryMapper.applyUpdate(category, request);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    public void delete(UUID userId, UUID categoryId) {
        Category category = requireOwned(userId, categoryId);
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public Category requireOwned(UUID userId, UUID categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }
}
