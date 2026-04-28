package com.example.delivery.payment.domain.entity;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.global.infrastructure.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 엔티티 — DB 테이블: p_payment
 *
 * [인덱스 설계]
 * - idx_payment_order_id (UNIQUE): order_id 기준 결제 단건 조회 및 중복 결제 방지.
 *   주문-결제가 1:1이므로 unique constraint 겸용.
 * - idx_payment_status: 상태별 결제 목록 조회(예: READY 상태 배치 재처리) 성능 확보.
 */
@Entity
@Table(
        name = "p_payment",
        indexes = {
                @Index(name = "idx_payment_order_id", columnList = "order_id", unique = true),
                @Index(name = "idx_payment_status",   columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID paymentId;

    /** 주문과 1:1 대응. UNIQUE 제약으로 중복 결제 방지. */
    @Column(name = "order_id", nullable = false, unique = true, columnDefinition = "UUID")
    private UUID orderId;

    /** 결제 수단. 현재 CARD만 지원하며 향후 확장 가능. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.CARD;

    /** 결제 상태 (READY → COMPLETED | FAILED, COMPLETED → CANCELLED). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.READY;

    /** 결제 금액. DB CHECK >= 0 — 엔티티 레벨에서도 동일하게 검증. */
    @Column(name = "amount", nullable = false)
    private Integer amount;

    /** PG사 거래 ID. 시뮬레이션에서는 UUID 기반 임의 값. 실패 시 null. */
    @Column(name = "pg_transaction_id", length = 100)
    private String pgTransactionId;

    /** 결제 완료 시각. COMPLETED 전환 시 자동 설정. */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Builder
    private PaymentEntity(UUID orderId, PaymentMethod paymentMethod, Integer amount) {
        if (amount == null || amount < 0) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT,
                    "결제 금액은 0원 이상이어야 합니다. 입력값: " + amount);
        }
        this.orderId = orderId;
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.CARD;
        this.amount = amount;
        this.status = PaymentStatus.READY;
    }

    // ─── 상태 전이 메서드 ───────────────────────────────────────

    /** PG 승인 성공 시 호출. status → COMPLETED, paidAt 설정. */
    public void markCompleted(String pgTransactionId) {
        this.status = PaymentStatus.COMPLETED;
        this.pgTransactionId = pgTransactionId;
        this.paidAt = LocalDateTime.now();
    }

    /** PG 승인 실패 시 호출. status → FAILED. */
    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    /** 결제 취소. COMPLETED 상태에서만 허용. */
    public void cancel() {
        if (this.status != PaymentStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS,
                    "완료된 결제만 취소할 수 있습니다. (현재 상태: %s)".formatted(this.status));
        }
        this.status = PaymentStatus.CANCELLED;
    }

    /**
     * FAILED 결제를 재시도 가능 상태로 초기화한다.
     * 기존 레코드를 재사용하므로 orderId UNIQUE 제약 충돌이 발생하지 않는다.
     */
    public void retry(Integer newAmount) {
        if (this.status != PaymentStatus.FAILED) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS,
                    "FAILED 상태의 결제만 재시도할 수 있습니다. (현재 상태: %s)".formatted(this.status));
        }
        if (newAmount == null || newAmount < 0) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT,
                    "결제 금액은 0원 이상이어야 합니다. 입력값: " + newAmount);
        }
        this.status = PaymentStatus.READY;
        this.amount = newAmount;
        this.pgTransactionId = null;
        this.paidAt = null;
    }
}