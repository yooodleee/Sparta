package com.example.delivery.area.presentation.dto.response;

import com.example.delivery.area.domain.entity.AreaEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "지역 조회 응답 DTO")
public record ResGetAreaDto(

        @Schema(description = "지역 ID")
        UUID areaId,

        @Schema(description = "지역명")
        String name,

        @Schema(description = "시/도")
        String city,

        @Schema(description = "구/군")
        String district,

        @Schema(description = "활성화 여부")
        boolean isActive,

        @Schema(description = "생성 시각")
        LocalDateTime createdAt,

        @Schema(description = "생성자")
        String createdBy,

        @Schema(description = "수정 시각")
        LocalDateTime updatedAt,

        @Schema(description = "수정자")
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
