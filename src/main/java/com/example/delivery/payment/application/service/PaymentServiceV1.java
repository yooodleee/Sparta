package com.example.delivery.payment.application.service;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.order.domain.repository.OrderRepository;
import com.example.delivery.payment.domain.entity.PaymentEntity;
import com.example.delivery.payment.domain.entity.PaymentStatus;
import com.example.delivery.payment.domain.repository.PaymentRepository;
import com.example.delivery.payment.infrastructure.pg.PgClient;
import com.example.delivery.payment.infrastructure.pg.PgResponse;
import com.example.delivery.payment.presentation.dto.request.ReqCreatePaymentDto;
import com.example.delivery.payment.presentation.dto.response.ResPaymentDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 서비스 V1.
 *
 * [의존성 흐름]
 * PaymentControllerV1 → PaymentServiceV1 → PaymentRepository (도메인 인터페이스)
 *                                         → OrderRepository   (주문 존재 검증)
 *                                         → PgClient          (PG 승인 인터페이스)
 *
 * [실행 흐름 — 결제 요청]
 * 1. 주문 존재 검증 (ORDER_NOT_FOUND)
 * 2. 중복 결제 검증 — 동일 orderId에 READY/COMPLETED 결제가 있으면 CONFLICT
 * 3. PaymentEntity 생성 (status=READY)
 * 4. PgClient.requestApproval() 호출 (시뮬레이션)
 * 5. 성공 → markCompleted(), 실패 → markFailed()
 * 6. DB 저장 후 응답 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceV1 {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PgClient pgClient;

    @Transactional
    public ResPaymentDto processPayment(ReqCreatePaymentDto dto) {
        // 1. 주문 존재 검증
        orderRepository.findById(dto.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 중복 결제 검증 — READY 또는 COMPLETED 상태인 결제가 이미 있으면 거부
        paymentRepository.findByOrderId(dto.orderId())
                .filter(p -> p.getStatus() == PaymentStatus.READY
                          || p.getStatus() == PaymentStatus.COMPLETED)
                .ifPresent(p -> {
                    throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
                });

        // 3. 결제 엔티티 생성 (초기 상태: READY)
        PaymentEntity payment = PaymentEntity.builder()
                .orderId(dto.orderId())
                .amount(dto.amount())
                .build();

        // 4. PG 승인 요청 (시뮬레이션)
        PgResponse pgResponse = pgClient.requestApproval(dto.orderId(), dto.amount());

        // 5. PG 응답에 따라 상태 전이
        if (pgResponse.success()) {
            payment.markCompleted(pgResponse.transactionId());
            log.info("[Payment] 결제 성공 — orderId={}, txId={}", dto.orderId(), pgResponse.transactionId());
        } else {
            payment.markFailed();
            log.warn("[Payment] 결제 실패 — orderId={}, reason={}", dto.orderId(), pgResponse.failReason());
        }

        // 6. 최종 상태로 저장
        PaymentEntity saved = paymentRepository.save(payment);
        return ResPaymentDto.from(saved);
    }

    public ResPaymentDto getPayment(UUID paymentId) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        return ResPaymentDto.from(payment);
    }

    public Page<ResPaymentDto> getPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable)
                .map(ResPaymentDto::from);
    }

    @Transactional
    public ResPaymentDto cancelPayment(UUID paymentId) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        payment.cancel();   // COMPLETED → CANCELLED, 그 외 상태면 BusinessException
        log.info("[Payment] 결제 취소 — paymentId={}", paymentId);
        return ResPaymentDto.from(payment);
    }
}
