package com.prioritize.mapper;

import org.springframework.stereotype.Component;

import com.prioritize.dto.CategoryRequest;
import com.prioritize.dto.CategoryResponse;
import com.prioritize.model.Category;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getIcon(),
                category.getColor(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }

    public void applyCreate(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setIcon(blankToNull(request.icon()));
        category.setColor(blankToNull(request.color()));
    }

    public void applyUpdate(Category category, CategoryRequest request) {
        applyCreate(category, request);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
