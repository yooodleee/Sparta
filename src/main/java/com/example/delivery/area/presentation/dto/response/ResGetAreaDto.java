package com.example.delivery.area.presentation.dto.response;

import com.example.delivery.area.domain.entity.AreaEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResGetAreaDto(
        UUID areaId,
        String name,
        String city,
        String district,
        boolean isActive,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
    public static ResGetAreaDto from(AreaEntity area) {
        return new ResGetAreaDto(
                area.getId(),
                area.getName(),
                area.getCity(),
                area.getDistrict(),
                area.isActive(),
                area.getCreatedAt(),
                area.getCreatedBy(),
                area.getUpdatedAt(),
                area.getUpdatedBy()
        );
    }
}
