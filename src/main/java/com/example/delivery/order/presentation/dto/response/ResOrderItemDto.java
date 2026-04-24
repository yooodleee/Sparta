package com.example.delivery.order.presentation.dto.response;

import com.example.delivery.order.domain.entity.OrderItemEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "주문 항목 응답 DTO")
public record ResOrderItemDto(
        UUID orderItemId,
        UUID menuId,
        Integer quantity,
        Integer unitPrice
) {
    public static ResOrderItemDto from(OrderItemEntity entity) {
        return new ResOrderItemDto(
                entity.getOrderItemId(),
                entity.getMenuId(),
                entity.getQuantity(),
                entity.getUnitPrice()
        );
    }
}
