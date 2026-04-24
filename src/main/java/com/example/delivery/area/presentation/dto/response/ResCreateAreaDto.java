package com.example.delivery.area.presentation.dto.response;

import com.example.delivery.area.domain.entity.AreaEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResCreateAreaDto(
        UUID areaId,
        String name,
        String city,
        String district,
        boolean isActive,
        LocalDateTime createdAt,
        String createdBy
) {
    public static ResCreateAreaDto from(AreaEntity area) {
        return new ResCreateAreaDto(
                area.getId(),
                area.getName(),
                area.getCity(),
                area.getDistrict(),
                area.isActive(),
                area.getCreatedAt(),
                area.getCreatedBy()
        );
    }
}
