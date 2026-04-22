package com.example.delivery.category.presentation.dto.response;

import com.example.delivery.category.domain.entity.CategoryEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResGetCategoryDto(
        UUID categoryId,
        String name,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
    public static ResGetCategoryDto from(CategoryEntity category) {
        return new ResGetCategoryDto(
                category.getId(),
                category.getName(),
                category.getCreatedAt(),
                category.getCreatedBy(),
                category.getUpdatedAt(),
                category.getUpdatedBy()
        );
    }
}
