package com.example.delivery.payment.domain.entity;

/**
 * 결제 상태 Enum
 *
 * READY     : 결제 요청 접수 (PG 호출 전 초기 상태)
 * COMPLETED : 결제 승인 완료
 * CANCELLED : 결제 취소 (완료된 결제에 한해 가능)
 * FAILED    : 결제 실패 (PG 거절 또는 내부 오류)
 */
public enum PaymentStatus {
    READY,
    COMPLETED,
    CANCELLED,
    FAILED
}
