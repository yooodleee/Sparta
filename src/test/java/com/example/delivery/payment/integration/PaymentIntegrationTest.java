package com.example.delivery.payment.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.delivery.order.domain.entity.OrderEntity;
import com.example.delivery.order.domain.repository.OrderRepository;
import com.example.delivery.payment.domain.entity.PaymentEntity;
import com.example.delivery.payment.domain.entity.PaymentStatus;
import com.example.delivery.payment.domain.repository.PaymentRepository;
import com.example.delivery.payment.infrastructure.pg.PgClient;
import com.example.delivery.payment.infrastructure.pg.PgResponse;
import com.example.delivery.payment.presentation.dto.request.ReqCreatePaymentDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 도메인 통합 테스트
 *
 * <p>[검증 목적]
 * <ul>
 *   <li>비즈니스 로직 → 유닛 테스트 커버됨 → DB 실제 저장·조회 확인</li>
 *   <li>order_id UNIQUE 인덱스 → FAILED 후 재결제 시 DB 제약 충돌 없음 확인</li>
 *   <li>트랜잭션 롤백 → PG 실패 시 FAILED 레코드가 실제로 커밋됐는지 확인</li>
 *   <li>HTTP 상태 코드 → 실제 DB 데이터로 페이지 크기·정렬 검증</li>
 *   <li>Security → 결제 API는 anyRequest().permitAll() 이므로 미인증 요청도 허용됨</li>
 * </ul>
 *
 * <p>[트랜잭션 격리]
 * 클래스 레벨 {@code @Transactional}로 각 테스트 후 자동 롤백한다.
 * 서비스의 {@code @Transactional(REQUIRED)}가 테스트 트랜잭션에 참여하므로
 * 테스트 픽스처와 서비스 저장 데이터가 동일 JPA 세션에서 공유된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PaymentRepository paymentRepository;
    @Autowired OrderRepository orderRepository;

    /**
     * SimulatedPgClient는 ThreadLocalRandom 기반(성공률 90%)이므로
     * 결정론적 검증을 위해 @MockBean으로 대체한다.
     * given(pgClient.requestApproval(...)).willReturn(...) 형태로 제어한다.
     */
    @MockBean PgClient pgClient;

    private static final int AMOUNT = 25_000;

    // ── 픽스쳐 헬퍼 ─────────────────────────────────────────────────────────

    /**
     * 실제 OrderEntity를 DB에 저장하고 반환한다.
     * PaymentServiceV1이 orderRepository.findById()로 주문 존재 여부를 검증하므로
     * 반드시 실제 DB에 저장된 Order가 필요하다.
     */
    private OrderEntity saveOrder() {
        return orderRepository.save(
                OrderEntity.builder()
                        .customerId("user01")
                        .storeId(UUID.randomUUID())
                        .totalPrice(AMOUNT)
                        .build()
        );
    }

    /**
     * 지정한 status의 PaymentEntity를 직접 DB에 저장하고 반환한다.
     *
     * <p>상태 전이:
     * <ul>
     *   <li>READY     : builder 기본값 유지 (별도 전이 없음)</li>
     *   <li>COMPLETED : markCompleted("SIM-tx-fixture") 호출</li>
     *   <li>FAILED    : markFailed() 호출</li>
     *   <li>CANCELLED : markCompleted() → cancel() 순서로 전이 후 저장</li>
     * </ul>
     */
    private PaymentEntity savePayment(UUID orderId, PaymentStatus status) {
        PaymentEntity p = PaymentEntity.builder()
                .orderId(orderId)
                .amount(AMOUNT)
                .build();
        switch (status) {
            case COMPLETED -> p.markCompleted("SIM-tx-fixture");
            case FAILED    -> p.markFailed();
            case CANCELLED -> { p.markCompleted("SIM-tx-fixture"); p.cancel(); }
            default        -> { /* READY — 빌더 기본 상태 */ }
        }
        return paymentRepository.save(p);
    }

    /** 결제 요청 JSON 문자열을 반환한다. */
    private String payJson(UUID orderId, int amount) throws Exception {
        return objectMapper.writeValueAsString(new ReqCreatePaymentDto(orderId, amount));
    }

    // ── 결제 요청 시나리오 ───────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/payments — 결제 요청")
    class ProcessPayment {

        @Test
        @DisplayName("정상 결제(Happy Path) — HTTP 201, COMPLETED 저장, paidAt 설정")
        void happyPath() throws Exception {
            // Given
            OrderEntity order = saveOrder();
            given(pgClient.requestApproval(any(UUID.class), anyInt()))
                    .willReturn(new PgResponse(true, "SIM-tx-001", null));

            // When & Then — HTTP 201
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(APPLICATION_JSON)
                            .content(payJson(order.getOrderId(), AMOUNT)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.pgTransactionId").value("SIM-tx-001"))
                    .andExpect(jsonPath("$.data.paidAt").isNotEmpty());

            // DB 검증 — p_payment 테이블에 COMPLETED 레코드 1건 존재
            assertThat(paymentRepository.findByOrderId(order.getOrderId()))
                    .isPresent()
                    .get()
                    .satisfies(p -> {
                        assertThat(p.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
                        assertThat(p.getPgTransactionId()).isEqualTo("SIM-tx-001");
                        assertThat(p.getPaidAt()).isNotNull();
                    });
        }

        @Test
        @DisplayName("PG 실패 → HTTP 201(비즈니스 이벤트), FAILED 레코드 커밋 확인")
        void pgFailure_savesFailed() throws Exception {
            // Given
            OrderEntity order = saveOrder();
            given(pgClient.requestApproval(any(UUID.class), anyInt()))
                    .willReturn(new PgResponse(false, null, "잔액 부족"));

            // When & Then
            // 결제 실패는 비즈니스 이벤트이므로 4xx/5xx가 아닌 201로 결과(FAILED)를 반환한다.
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(APPLICATION_JSON)
                            .content(payJson(order.getOrderId(), AMOUNT)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.status").value("FAILED"))
                    .andExpect(jsonPath("$.data.pgTransactionId").doesNotExist())
                    .andExpect(jsonPath("$.data.paidAt").doesNotExist());

            // DB 검증 — PG 실패 시 FAILED 레코드가 실제로 커밋됐는지 확인
            assertThat(paymentRepository.findByOrderId(order.getOrderId()))
                    .isPresent()
                    .get()
                    .satisfies(p -> {
                        assertThat(p.getStatus()).isEqualTo(PaymentStatus.FAILED);
                        assertThat(p.getPgTransactionId()).isNull();
                        assertThat(p.getPaidAt()).isNull();
                    });
        }

        @Test
        @DisplayName("FAILED 후 재결제 — order_id UNIQUE 제약 충돌 없이 COMPLETED 저장")
        void retryAfterFailed_succeeds() throws Exception {
            // Given — 동일 orderId로 FAILED 결제가 이미 존재
            OrderEntity order = saveOrder();
            savePayment(order.getOrderId(), PaymentStatus.FAILED);

            given(pgClient.requestApproval(any(UUID.class), anyInt()))
                    .willReturn(new PgResponse(true, "SIM-tx-002", null));

            // When & Then
            // [설계 검증 포인트]
            // 서비스가 재결제 시 새 PaymentEntity를 INSERT하는 현재 구현에서는
            // order_id UNIQUE 제약으로 DataIntegrityViolationException(→ HTTP 500)이 발생한다.
            // 이 테스트가 실패한다면 서비스 수정이 필요하다.
            //   수정 방향 A: 기존 FAILED 레코드를 UPDATE(reset → mark)하는 방식으로 전환
            //   수정 방향 B: order_id에 partial UNIQUE index 적용 (COMPLETED/READY에만 제약)
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(APPLICATION_JSON)
                            .content(payJson(order.getOrderId(), AMOUNT)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));

            // DB 검증 — 최종 결제 레코드가 COMPLETED 상태인지 확인
            assertThat(paymentRepository.findByOrderId(order.getOrderId()))
                    .isPresent()
                    .get()
                    .satisfies(p -> assertThat(p.getStatus()).isEqualTo(PaymentStatus.COMPLETED));
        }

        @Test
        @DisplayName("중복 결제 방지 — COMPLETED 존재 → HTTP 409, PgClient 호출 0회")
        void duplicatePrevention_completed_returns409() throws Exception {
            // Given
            OrderEntity order = saveOrder();
            savePayment(order.getOrderId(), PaymentStatus.COMPLETED);

            // When & Then
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(APPLICATION_JSON)
                            .content(payJson(order.getOrderId(), AMOUNT)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message")
                            .value("해당 주문에 이미 처리된 결제가 존재합니다."));

            // 불필요한 PG 호출 방지 검증 — 중복 감지 시 PG에 요청하지 않아야 한다
            verify(pgClient, never()).requestApproval(any(), anyInt());
        }

        @Test
        @DisplayName("중복 결제 방지 — READY 존재 → HTTP 409, PgClient 호출 0회")
        void duplicatePrevention_ready_returns409() throws Exception {
            // Given — PaymentEntity 빌더 기본 상태가 READY
            OrderEntity order = saveOrder();
            savePayment(order.getOrderId(), PaymentStatus.READY);

            // When & Then
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(APPLICATION_JSON)
                            .content(payJson(order.getOrderId(), AMOUNT)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message")
                            .value("해당 주문에 이미 처리된 결제가 존재합니다."));

            verify(pgClient, never()).requestApproval(any(), anyInt());
        }

        @Test
        @DisplayName("없는 주문으로 결제 시도 → HTTP 404, DB 레코드 미생성")
        void orderNotFound_returns404() throws Exception {
            // Given
            UUID nonExistentOrderId = UUID.randomUUID();

            // When & Then
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(APPLICATION_JSON)
                            .content(payJson(nonExistentOrderId, AMOUNT)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("주문을 찾을 수 없습니다."));

            // DB 검증 — 아무 레코드도 생성되지 않음
            assertThat(paymentRepository.findByOrderId(nonExistentOrderId)).isEmpty();
        }

        @Nested
        @DisplayName("입력 값 유효성 검사")
        class Validation {

            @Test
            @DisplayName("orderId null → HTTP 400 (Bean Validation)")
            void nullOrderId_returns400() throws Exception {
                mockMvc.perform(post("/api/v1/payments")
                                .contentType(APPLICATION_JSON)
                                .content("{\"orderId\": null, \"amount\": 25000}"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("VALIDATION_ERROR"));
            }

            @Test
            @DisplayName("amount = -1 → HTTP 400 (@Min(0) 위반)")
            void negativeAmount_returns400() throws Exception {
                mockMvc.perform(post("/api/v1/payments")
                                .contentType(APPLICATION_JSON)
                                .content("{\"orderId\": \"" + UUID.randomUUID()
                                        + "\", \"amount\": -1}"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("VALIDATION_ERROR"));
            }
        }
    }

    // ── 결제 단건 조회 시나리오 ──────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/payments/{id} — 결제 단건 조회")
    class GetPayment {

        @Test
        @DisplayName("COMPLETED 결제 조회 → HTTP 200, orderId·status·amount 일치")
        void getExistingPayment_returns200() throws Exception {
            // Given
            OrderEntity order = saveOrder();
            PaymentEntity payment = savePayment(order.getOrderId(), PaymentStatus.COMPLETED);

            // When & Then
            mockMvc.perform(get("/api/v1/payments/{id}", payment.getPaymentId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.paymentId").value(payment.getPaymentId().toString()))
                    .andExpect(jsonPath("$.data.orderId").value(order.getOrderId().toString()))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.amount").value(AMOUNT));
        }

        @Test
        @DisplayName("없는 결제 조회 → HTTP 404 (PAYMENT_NOT_FOUND)")
        void getNonExistentPayment_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/payments/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("결제 정보를 찾을 수 없습니다."));
        }
    }

    // ── 결제 목록 조회(페이지네이션) 시나리오 ───────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/payments — 결제 목록 조회")
    class GetPayments {

        @Test
        @DisplayName("결제 3건 저장 후 size=2 요청 → content=2건, totalElements=3, totalPages=2")
        void pagination_threeRecords_pageSizeTwo() throws Exception {
            // Given — 서로 다른 orderId로 결제 3건 저장
            for (int i = 0; i < 3; i++) {
                OrderEntity order = saveOrder();
                savePayment(order.getOrderId(), PaymentStatus.COMPLETED);
            }

            // When & Then
            mockMvc.perform(get("/api/v1/payments")
                            .param("page", "0")
                            .param("size", "2")
                            .param("sort", "createdAt,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.totalElements").value(3))
                    .andExpect(jsonPath("$.data.totalPages").value(2));
        }
    }

    // ── 결제 취소 시나리오 ───────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/payments/{id}/cancel — 결제 취소")
    class CancelPayment {

        @Test
        @DisplayName("정상 취소 흐름 — COMPLETED → CANCELLED, HTTP 200, DB status 변경 확인")
        void cancelCompleted_returns200() throws Exception {
            // Given
            OrderEntity order = saveOrder();
            PaymentEntity payment = savePayment(order.getOrderId(), PaymentStatus.COMPLETED);

            // When & Then
            mockMvc.perform(post("/api/v1/payments/{id}/cancel", payment.getPaymentId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"));

            // DB 검증 — status가 실제로 CANCELLED로 변경됐는지 확인
            assertThat(paymentRepository.findById(payment.getPaymentId()))
                    .isPresent()
                    .get()
                    .satisfies(p -> assertThat(p.getStatus()).isEqualTo(PaymentStatus.CANCELLED));
        }

        @Test
        @DisplayName("FAILED 결제 취소 시도 → HTTP 400 (INVALID_PAYMENT_STATUS), DB status 변경 없음")
        void cancelFailed_returns400() throws Exception {
            // Given
            OrderEntity order = saveOrder();
            PaymentEntity payment = savePayment(order.getOrderId(), PaymentStatus.FAILED);

            // When & Then
            mockMvc.perform(post("/api/v1/payments/{id}/cancel", payment.getPaymentId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("현재 결제 상태에서는 해당 작업을 수행할 수 없습니다."));

            // DB 검증 — status 변경 없음
            assertThat(paymentRepository.findById(payment.getPaymentId()))
                    .isPresent()
                    .get()
                    .satisfies(p -> assertThat(p.getStatus()).isEqualTo(PaymentStatus.FAILED));
        }

        @Test
        @DisplayName("CANCELLED 결제 재취소 시도 → HTTP 400 (INVALID_PAYMENT_STATUS)")
        void cancelAlreadyCancelled_returns400() throws Exception {
            // Given — COMPLETED → cancel() 경로로 CANCELLED 상태 저장
            OrderEntity order = saveOrder();
            PaymentEntity payment = savePayment(order.getOrderId(), PaymentStatus.CANCELLED);

            // When & Then
            mockMvc.perform(post("/api/v1/payments/{id}/cancel", payment.getPaymentId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("현재 결제 상태에서는 해당 작업을 수행할 수 없습니다."));
        }
    }
}
