package com.example.delivery.order.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "주문 항목 요청 DTO")
public record ReqOrderItemDto(

        @Schema(description = "메뉴 ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull(message = "메뉴 ID는 필수입니다.")
        UUID menuId,

        @Schema(description = "수량", example = "2")
        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        Integer quantity,

        @Schema(description = "단가", example = "18000")
        @NotNull(message = "단가는 필수입니다.")
        @Min(value = 0, message = "단가는 0원 이상이어야 합니다.")
        Integer unitPrice
) {
}
