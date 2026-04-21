package com.example.delivery.payment.presentation.dto.response;

import com.example.delivery.payment.domain.entity.PaymentEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "결제 응답 DTO")
public record ResPaymentDto(

        @Schema(description = "결제 ID")
        UUID paymentId,

        @Schema(description = "주문 ID")
        UUID orderId,

        @Schema(description = "결제 수단", example = "CARD")
        String paymentMethod,

        @Schema(description = "결제 상태", example = "COMPLETED")
        String status,

        @Schema(description = "결제 금액", example = "25000")
        Integer amount,

        @Schema(description = "PG사 거래 ID (성공 시에만 존재)")
        String pgTransactionId,

        @Schema(description = "결제 완료 시각 (성공 시에만 존재)")
        LocalDateTime paidAt,

        @Schema(description = "결제 생성 시각")
        LocalDateTime createdAt
) {
    public static ResPaymentDto from(PaymentEntity entity) {
        return new ResPaymentDto(
                entity.getPaymentId(),
                entity.getOrderId(),
                entity.getPaymentMethod().name(),
                entity.getStatus().name(),
                entity.getAmount(),
                entity.getPgTransactionId(),
                entity.getPaidAt(),
                entity.getCreatedAt()
        );
    }
}
