package com.example.delivery.payment.infrastructure.pg;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * PG사 시뮬레이션 구현체.
 *
 * 실제 외부 네트워크 호출 없이 내부적으로 결제 성공/실패를 결정한다.
 * - 성공률: 90% (ThreadLocalRandom 기반)
 * - 거래 ID: "SIM-" + UUID (시뮬레이션 접두사로 실제 PG와 구분)
 *
 * [실제 PG 연동으로 전환 시]
 * 이 클래스 대신 실제 HTTP 클라이언트(RestClient, WebClient 등)를 사용하는
 * 구현체를 작성하고, @Primary 또는 @Profile("prod")로 교체한다.
 * PaymentServiceV1은 PgClient 인터페이스에만 의존하므로 수정 불필요.
 */
@Slf4j
@Component
public class SimulatedPgClient implements PgClient {

    private static final double SUCCESS_RATE = 0.9;

    @Override
    public PgResponse requestApproval(UUID orderId, int amount) {
        log.info("[SimulatedPG] 결제 승인 요청 — orderId={}, amount={}", orderId, amount);

        boolean success = ThreadLocalRandom.current().nextDouble() < SUCCESS_RATE;

        if (success) {
            String transactionId = "SIM-" + UUID.randomUUID();
            log.info("[SimulatedPG] 승인 성공 — transactionId={}", transactionId);
            return PgResponse.success(transactionId);
        }

        log.warn("[SimulatedPG] 승인 실패 — orderId={}", orderId);
        return PgResponse.failure("시뮬레이션 결제 거절");
    }
}
