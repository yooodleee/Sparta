package com.example.delivery.category.presentation.dto.response;

import com.example.delivery.category.domain.entity.CategoryEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "카테고리 조회 응답 DTO")
public record ResGetCategoryDto(

        @Schema(description = "카테고리 ID")
        UUID categoryId,

        @Schema(description = "카테고리 이름")
        String name,

        @Schema(description = "생성 시각")
        LocalDateTime createdAt,

        @Schema(description = "생성자")
        String createdBy,

        @Schema(description = "수정 시각")
        LocalDateTime updatedAt,

        @Schema(description = "수정자")
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
