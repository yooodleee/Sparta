package com.example.delivery.menu.presentation.dto.response;
import com.example.delivery.menu.domain.entity.MenuEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "메뉴(상품) 응답 DTO")
public record ResMenuDto(
        UUID id,
        UUID storeId,
        String name,
        String description,
        Integer price,
        Boolean isHidden,
        Boolean isSoldOut,
        String imageUrl,
        Boolean aiDescription
) {
    public static ResMenuDto from(MenuEntity entity) {
        return new ResMenuDto(
                entity.getId(),
                entity.getStoreId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getIsHidden(),
                entity.getIsSoldOut(),
                entity.getImageUrl(),
                entity.getAiDescription()
        );
    }
}



