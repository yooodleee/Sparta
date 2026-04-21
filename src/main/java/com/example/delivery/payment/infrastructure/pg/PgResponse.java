package com.example.delivery.payment.infrastructure.pg;

/**
 * PG사 승인 응답 모델.
 *
 * @param success       승인 성공 여부
 * @param transactionId PG사 거래 고유 ID (성공 시 non-null)
 * @param failReason    실패 사유 (실패 시 non-null)
 */
public record PgResponse(
        boolean success,
        String transactionId,
        String failReason
) {
    public static PgResponse success(String transactionId) {
        return new PgResponse(true, transactionId, null);
    }

    public static PgResponse failure(String failReason) {
        return new PgResponse(false, null, failReason);
    }
}
