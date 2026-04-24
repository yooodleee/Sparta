package com.example.delivery.store.presentation.dto.response;

import com.example.delivery.store.domain.entity.StoreEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ResGetStoreDto(
        UUID storeId,
        UUID ownerId,
        UUID categoryId,
        UUID areaId,
        String name,
        String address,
        String phone,
        BigDecimal averageRating,
        boolean isHidden,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
    public static ResGetStoreDto from(StoreEntity store) {
        return new ResGetStoreDto(
                store.getId(),
                store.getOwnerId(),
                store.getCategoryId(),
                store.getAreaId(),
                store.getName(),
                store.getAddress(),
                store.getPhone(),
                store.getAverageRating(),
                store.isHidden(),
                store.getCreatedAt(),
                store.getCreatedBy(),
                store.getUpdatedAt(),
                store.getUpdatedBy()
        );
    }
}
