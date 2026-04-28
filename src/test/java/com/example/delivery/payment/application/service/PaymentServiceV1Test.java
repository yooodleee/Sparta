package com.example.delivery.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyInt;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.order.domain.entity.OrderEntity;
import com.example.delivery.order.domain.repository.OrderRepository;
import static org.mockito.Mockito.mock;
import com.example.delivery.payment.domain.entity.PaymentEntity;
import com.example.delivery.payment.domain.entity.PaymentStatus;
import com.example.delivery.payment.domain.repository.PaymentRepository;
import com.example.delivery.payment.infrastructure.pg.PgClient;
import com.example.delivery.payment.infrastructure.pg.PgResponse;
import com.example.delivery.payment.presentation.dto.request.ReqCreatePaymentDto;
import com.example.delivery.payment.presentation.dto.response.ResPaymentDto;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceV1Test {

    @Mock PaymentRepository paymentRepository;
    @Mock OrderRepository   orderRepository;
    @Mock PgClient          pgClient;

    @InjectMocks PaymentServiceV1 paymentService;

    // ─── 공통 픽스처 ───────────────────────────────────────────
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final int  AMOUNT   = 25_000;

    private ReqCreatePaymentDto validRequest() {
        return new ReqCreatePaymentDto(ORDER_ID, AMOUNT);
    }

    private PaymentEntity buildPayment(PaymentStatus status) {
        PaymentEntity p = PaymentEntity.builder()
                .orderId(ORDER_ID)
                .amount(AMOUNT)
                .build();
        if (status == PaymentStatus.COMPLETED) {
            p.markCompleted("SIM-test-tx");
        } else if (status == PaymentStatus.FAILED) {
            p.markFailed();
        }
        return p;
    }

    // ─── processPayment ────────────────────────────────────────
    @Nested
    @DisplayName("processPayment — 결제 요청")
    class ProcessPayment {

        @Test
        @DisplayName("PG 승인 성공 → COMPLETED 상태로 저장")
        void success_completed() {
            // given
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(mock(OrderEntity.class)));
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.empty());
            given(pgClient.requestApproval(eq(ORDER_ID), eq(AMOUNT)))
                    .willReturn(PgResponse.success("SIM-abc-123"));
            given(paymentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            ResPaymentDto result = paymentService.processPayment(validRequest());

            // then
            assertThat(result.status()).isEqualTo(PaymentStatus.COMPLETED.name());
            assertThat(result.pgTransactionId()).isEqualTo("SIM-abc-123");
            assertThat(result.paidAt()).isNotNull();
            verify(paymentRepository).save(any(PaymentEntity.class));
        }

        @Test
        @DisplayName("PG 승인 실패 → FAILED 상태로 저장")
        void pgFailed_statusFailed() {
            // given
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(mock(OrderEntity.class)));
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.empty());
            given(pgClient.requestApproval(eq(ORDER_ID), eq(AMOUNT)))
                    .willReturn(PgResponse.failure("잔액 부족"));
            given(paymentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            ResPaymentDto result = paymentService.processPayment(validRequest());

            // then
            assertThat(result.status()).isEqualTo(PaymentStatus.FAILED.name());
            assertThat(result.pgTransactionId()).isNull();
            assertThat(result.paidAt()).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 주문 → ORDER_NOT_FOUND 예외")
        void orderNotFound_throwsException() {
            // given
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> paymentService.processPayment(validRequest()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_NOT_FOUND));

            verify(pgClient, never()).requestApproval(any(), anyInt());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("COMPLETED 결제가 이미 존재 → PAYMENT_ALREADY_PROCESSED 예외")
        void duplicateCompleted_throwsException() {
            // given
            PaymentEntity existing = buildPayment(PaymentStatus.COMPLETED);
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(mock(OrderEntity.class)));
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(existing));

            // when / then
            assertThatThrownBy(() -> paymentService.processPayment(validRequest()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.PAYMENT_ALREADY_PROCESSED));

            verify(pgClient, never()).requestApproval(any(), anyInt());
        }

        @Test
        @DisplayName("READY 결제가 이미 존재 → PAYMENT_ALREADY_PROCESSED 예외")
        void duplicateReady_throwsException() {
            // given
            PaymentEntity existing = buildPayment(PaymentStatus.READY);
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(mock(OrderEntity.class)));
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(existing));

            // when / then
            assertThatThrownBy(() -> paymentService.processPayment(validRequest()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.PAYMENT_ALREADY_PROCESSED));
        }

        @Test
        @DisplayName("FAILED 결제가 존재해도 재결제 가능 → 정상 처리")
        void retryAfterFailed_succeeds() {
            // given: 이전 결제가 FAILED 상태
            PaymentEntity failedPayment = buildPayment(PaymentStatus.FAILED);
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(mock(OrderEntity.class)));
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(failedPayment));
            given(pgClient.requestApproval(eq(ORDER_ID), eq(AMOUNT)))
                    .willReturn(PgResponse.success("SIM-retry-tx"));
            given(paymentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            ResPaymentDto result = paymentService.processPayment(validRequest());

            // then
            assertThat(result.status()).isEqualTo(PaymentStatus.COMPLETED.name());
        }
    }

    // ─── getPayment ─────────────────────────────────────────────
    @Nested
    @DisplayName("getPayment — 결제 단건 조회")
    class GetPayment {

        @Test
        @DisplayName("존재하는 결제 ID → 정상 반환")
        void found_returnsDto() {
            // given
            UUID paymentId = UUID.randomUUID();
            PaymentEntity payment = buildPayment(PaymentStatus.COMPLETED);
            given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

            // when
            ResPaymentDto result = paymentService.getPayment(paymentId);

            // then
            assertThat(result.status()).isEqualTo(PaymentStatus.COMPLETED.name());
            assertThat(result.orderId()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("존재하지 않는 결제 ID → PAYMENT_NOT_FOUND 예외")
        void notFound_throwsException() {
            // given
            UUID paymentId = UUID.randomUUID();
            given(paymentRepository.findById(paymentId)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> paymentService.getPayment(paymentId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND));
        }
    }

    // ─── cancelPayment ──────────────────────────────────────────
    @Nested
    @DisplayName("cancelPayment — 결제 취소")
    class CancelPayment {

        @Test
        @DisplayName("PG 환불 성공 → CANCELLED 상태")
        void completedPayment_cancelSucceeds() {
            // given
            UUID paymentId = UUID.randomUUID();
            PaymentEntity payment = buildPayment(PaymentStatus.COMPLETED);
            given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
            given(pgClient.requestRefund(any(), anyInt()))
                    .willReturn(PgResponse.success("SIM-refund-tx"));

            // when
            ResPaymentDto result = paymentService.cancelPayment(paymentId);

            // then
            assertThat(result.status()).isEqualTo(PaymentStatus.CANCELLED.name());
            verify(pgClient).requestRefund(any(), anyInt());
        }

        @Test
        @DisplayName("PG 환불 실패 → REFUND_FAILED 예외, PG 호출 1회 확인")
        void pgRefundFailed_throwsRefundFailed() {
            // given
            UUID paymentId = UUID.randomUUID();
            PaymentEntity payment = buildPayment(PaymentStatus.COMPLETED);
            given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
            given(pgClient.requestRefund(any(), anyInt()))
                    .willReturn(PgResponse.failure("잔액 부족"));

            // when / then
            assertThatThrownBy(() -> paymentService.cancelPayment(paymentId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.REFUND_FAILED));

            verify(pgClient).requestRefund(any(), anyInt());
        }

        @Test
        @DisplayName("FAILED 결제 취소 시도 → INVALID_PAYMENT_STATUS 예외, PG 호출 없음")
        void failedPayment_cancelThrowsException() {
            // given
            UUID paymentId = UUID.randomUUID();
            PaymentEntity payment = buildPayment(PaymentStatus.FAILED);
            given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

            // when / then
            assertThatThrownBy(() -> paymentService.cancelPayment(paymentId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS));

            verify(pgClient, never()).requestRefund(any(), anyInt());
        }
    }
}
