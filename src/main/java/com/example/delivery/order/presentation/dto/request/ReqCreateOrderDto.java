package com.example.delivery.order.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "주문 생성 요청 DTO")
public record ReqCreateOrderDto(

        @Schema(description = "가게 ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull(message = "가게 ID는 필수입니다.")
        UUID storeId,

        @Schema(description = "배송지 ID (매장 주문이면 null)", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID addressId,

        @Schema(description = "요청사항", example = "문 앞에 두고 가주세요.")
        @Size(max = 500, message = "요청사항은 최대 500자까지 가능합니다.")
        String request,

        @Schema(description = "주문 항목 목록")
        @NotEmpty(message = "주문 항목은 최소 1개 이상이어야 합니다.")
        @Valid
        List<ReqOrderItemDto> items
) {
}
