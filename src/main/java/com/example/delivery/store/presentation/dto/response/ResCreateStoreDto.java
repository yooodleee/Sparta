package com.example.delivery.store.presentation.dto.response;

import com.example.delivery.store.domain.entity.StoreEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "가게 생성 응답 DTO")
public record ResCreateStoreDto(

        @Schema(description = "가게 ID")
        UUID storeId,

        @Schema(description = "소유자(OWNER) ID")
        UUID ownerId,

        @Schema(description = "카테고리 ID")
        UUID categoryId,

        @Schema(description = "지역 ID")
        UUID areaId,

        @Schema(description = "가게명")
        String name,

        @Schema(description = "가게 주소")
        String address,

        @Schema(description = "전화번호")
        String phone,

        @Schema(description = "최소 주문 금액")
        Integer minOrderAmount,

        @Schema(description = "평균 평점")
        BigDecimal averageRating,

        @Schema(description = "숨김 여부")
        boolean isHidden,

        @Schema(description = "생성 시각")
        LocalDateTime createdAt,

        @Schema(description = "생성자")
        String createdBy
) {
    public static ResCreateStoreDto from(StoreEntity store) {
        return new ResCreateStoreDto(
                store.getId(),
                store.getOwnerId(),
                store.getCategoryId(),
                store.getAreaId(),
                store.getName(),
                store.getAddress(),
                store.getPhone(),
                store.getMinOrderAmount(),
                store.getAverageRating(),
                store.isHidden(),
                store.getCreatedAt(),
                store.getCreatedBy()
        );
    }
}
