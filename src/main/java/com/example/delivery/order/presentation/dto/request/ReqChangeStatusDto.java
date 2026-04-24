package com.example.delivery.order.presentation.dto.request;

import com.example.delivery.order.domain.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "주문 상태 변경 요청 DTO")
public record ReqChangeStatusDto(

        @Schema(description = "변경할 주문 상태", example = "ACCEPTED")
        @NotNull(message = "상태는 필수입니다.")
        OrderStatus status
) {
}
