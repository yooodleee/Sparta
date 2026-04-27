package com.example.delivery.order.presentation.dto.response;

import com.example.delivery.order.domain.entity.OrderEntity;
import com.example.delivery.order.domain.entity.OrderStatus;
import com.example.delivery.order.domain.entity.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "주문 응답 DTO")
public record ResOrderDto(
        UUID orderId,
        String customerId,
        UUID storeId,
        UUID addressId,
        OrderType orderType,
        OrderStatus status,
        Integer totalPrice,
        String request,
        LocalDateTime acceptedAt,
        LocalDateTime deliveredAt,
        LocalDateTime canceledAt,
        List<ResOrderItemDto> items
) {
    public static ResOrderDto from(OrderEntity entity) {
        return new ResOrderDto(
                entity.getOrderId(),
                entity.getCustomerId(),
                entity.getStoreId(),
                entity.getAddressId(),
                entity.getOrderType(),
                entity.getStatus(),
                entity.getTotalPrice(),
                entity.getRequest(),
                entity.getAcceptedAt(),
                entity.getDeliveredAt(),
                entity.getCanceledAt(),
                entity.getItems().stream().map(ResOrderItemDto::from).toList()
        );
    }
}
