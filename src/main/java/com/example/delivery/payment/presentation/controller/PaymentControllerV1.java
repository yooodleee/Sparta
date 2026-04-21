package com.example.delivery.payment.presentation.controller;

import com.example.delivery.global.common.response.ApiResponse;
import com.example.delivery.payment.application.service.PaymentServiceV1;
import com.example.delivery.payment.presentation.dto.request.ReqCreatePaymentDto;
import com.example.delivery.payment.presentation.dto.response.ResPaymentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "결제 API", description = "결제 요청 및 조회")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentControllerV1 {

    private final PaymentServiceV1 paymentService;

    /**
     * 결제 요청 — POST /api/v1/payments
     *
     * 요청 바디의 orderId·amount를 받아 내부적으로 PG 승인을 시뮬레이션한다.
     * 성공 시 HTTP 201, 실패 시에도 HTTP 200으로 결과(FAILED)를 반환한다.
     * (결제 실패는 비즈니스 이벤트이므로 4xx/5xx로 처리하지 않는다.)
     */
    @Operation(summary = "결제 요청", description = "주문 ID와 금액으로 카드 결제를 요청합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ResPaymentDto>> processPayment(
            @Valid @RequestBody ReqCreatePaymentDto request
    ) {
        ResPaymentDto result = paymentService.processPayment(request);
        return ResponseEntity
                .status(201)
                .body(ApiResponse.created(result));
    }

    /**
     * 결제 단건 조회 — GET /api/v1/payments/{id}
     */
    @Operation(summary = "결제 단건 조회")
    @GetMapping("/{id}")
    public ApiResponse<ResPaymentDto> getPayment(@PathVariable UUID id) {
        return ApiResponse.ok(paymentService.getPayment(id));
    }

    /**
     * 결제 목록 조회 — GET /api/v1/payments?page=0&size=10&sort=createdAt,desc
     */
    @Operation(summary = "결제 목록 조회", description = "페이지네이션을 지원합니다.")
    @GetMapping
    public ApiResponse<Page<ResPaymentDto>> getPayments(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        return ApiResponse.ok(paymentService.getPayments(pageable));
    }

    /**
     * 결제 취소 — POST /api/v1/payments/{id}/cancel
     *
     * COMPLETED 상태의 결제를 CANCELLED로 전환한다.
     */
    @Operation(summary = "결제 취소", description = "완료된 결제를 취소합니다.")
    @PostMapping("/{id}/cancel")
    public ApiResponse<ResPaymentDto> cancelPayment(@PathVariable UUID id) {
        return ApiResponse.ok(paymentService.cancelPayment(id));
    }
}
