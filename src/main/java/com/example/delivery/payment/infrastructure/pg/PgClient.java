package com.example.delivery.payment.infrastructure.pg;

import java.util.UUID;

/**
 * PG사 연동 클라이언트 인터페이스.
 *
 * [확장 설계]
 * - 현재: SimulatedPgClient (내부 시뮬레이션)
 * - 실제 PG 연동 시: KakaoPgClient, TossPgClient 등을 이 인터페이스의
 *   구현체로 추가하고, @Primary 또는 @ConditionalOnProperty로 전환.
 * - PaymentServiceV1은 이 인터페이스에만 의존하므로 서비스 코드 변경 불필요.
 */
public interface PgClient {

    /**
     * PG사에 결제 승인 요청.
     *
     * @param orderId 주문 ID
     * @param amount  결제 금액
     * @return 승인 응답 (성공/실패, 거래 ID, 실패 사유)
     */
    PgResponse requestApproval(UUID orderId, int amount);
}
