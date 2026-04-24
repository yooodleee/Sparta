package com.example.delivery.category.presentation.dto.response;

import com.example.delivery.category.domain.entity.CategoryEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResCreateCategoryDto(
        UUID categoryId,
        String name,
        LocalDateTime createdAt,
        String createdBy
) {
    public static ResCreateCategoryDto from(CategoryEntity category) {
        return new ResCreateCategoryDto(
                category.getId(),
                category.getName(),
                category.getCreatedAt(),
                category.getCreatedBy()
        );
    }
}
