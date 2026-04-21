package com.example.delivery.payment.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "결제 요청 DTO")
public record ReqCreatePaymentDto(

        @Schema(description = "결제할 주문 ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        @Schema(description = "결제 금액 (0원 이상)", example = "25000")
        @NotNull(message = "결제 금액은 필수입니다.")
        @Min(value = 0, message = "결제 금액은 0원 이상이어야 합니다.")
        Integer amount
) {
}
