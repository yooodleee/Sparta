package com.example.delivery.order.presentation.controller;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.global.common.response.ApiResponse;
import com.example.delivery.global.infrastructure.security.UserPrincipal;
import com.example.delivery.order.application.service.OrderServiceV1;
import com.example.delivery.order.domain.entity.OrderStatus;
import com.example.delivery.order.presentation.dto.request.ReqChangeStatusDto;
import com.example.delivery.order.presentation.dto.request.ReqCreateOrderDto;
import com.example.delivery.order.presentation.dto.request.ReqUpdateRequestDto;
import com.example.delivery.order.presentation.dto.response.ResOrderDto;
import com.example.delivery.user.domain.entity.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주문 컨트롤러 V1.
 *
 * [역할 분담]
 * 서비스 레이어의 메서드는 역할별로 나뉘어 있으므로 컨트롤러가 {@link UserPrincipal#role()}을 보고
 * 올바른 서비스 메서드로 디스패치(허용되지 않은 role은 403 FORBIDDEN.)
 *
 * [본인 확인]
 * CUSTOMER 전용 경로(cancelByCustomer / updateRequestByCustomer)에서는 요청자의 username을
 * 서비스로 그대로 전달하고, {@link com.example.delivery.order.domain.entity.OrderEntity}가
 * {@code customerId}와 비교해 FORBIDDEN을 던진다. 컨트롤러는 role 디스패치만 담당.
 *
 * [응답 규약]
 * 모든 응답은 {@link ApiResponse}로 감싸며, 예외는 GlobalExceptionHandler에서 공통 포맷으로
 * 변환된다.
 */
@Tag(name = "Order", description = "주문 API")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderControllerV1 {

    private final OrderServiceV1 orderService;

    @Operation(summary = "주문 생성", description = "CUSTOMER가 장바구니 항목으로 주문을 생성합니다. (인증 필요)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "주문 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 본문 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CUSTOMER 권한 아님")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResOrderDto> createOrder(
            @RequestBody @Valid ReqCreateOrderDto request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireRole(principal, UserRole.CUSTOMER);
        return ApiResponse.created(orderService.createOrder(principal.username(), request));
    }

    @Operation(summary = "주문 단건 조회", description = "주문 ID로 주문을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 없음")
    })
    @GetMapping("/{orderId}")
    public ApiResponse<ResOrderDto> getOrder(
            @Parameter(description = "주문 ID") @PathVariable UUID orderId
    ) {
        return ApiResponse.ok(orderService.getOrder(orderId));
    }


    @Operation(summary = "주문 목록 조회", description = "페이지네이션으로 주문 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<Page<ResOrderDto>> getOrders(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.ok(orderService.getOrders(pageable));
    }


    @Operation(summary = "주문 취소",
            description = "CUSTOMER는 본인 주문 한정(PENDING + 5분 이내), MASTER는 제약 없이 취소합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "상태/시간 조건 불충족"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 없음")
    })
    @PostMapping("/{orderId}/cancel")
    public ApiResponse<ResOrderDto> cancelOrder(
            @Parameter(description = "주문 ID") @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return switch (principal.role()) {
            case MASTER -> ApiResponse.ok(orderService.cancelByMaster(orderId));
            case CUSTOMER -> ApiResponse.ok(orderService.cancelByCustomer(orderId, principal.username()));
            default -> throw new BusinessException(ErrorCode.FORBIDDEN);
        };
    }


    @Operation(summary = "주문 상태 변경",
            description = "OWNER는 정방향 전이, MANAGER는 CANCELLED 제외 자유 전이, MASTER는 제약 없음.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "허용되지 않는 상태 전이"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 없음")
    })
    @PatchMapping("/{orderId}/status")
    public ApiResponse<ResOrderDto> changeStatus(
            @Parameter(description = "주문 ID") @PathVariable UUID orderId,
            @RequestBody @Valid ReqChangeStatusDto request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OrderStatus next = request.status();
        return switch (principal.role()) {
            case OWNER -> ApiResponse.ok(orderService.changeStatusByOwner(orderId, next));
            case MANAGER -> ApiResponse.ok(orderService.changeStatusByManager(orderId, next));
            case MASTER -> ApiResponse.ok(orderService.changeStatusByMaster(orderId, next));
            default -> throw new BusinessException(ErrorCode.FORBIDDEN);
        };
    }

    @Operation(summary = "주문 요청사항 수정",
            description = "PENDING 상태에서만 수정 가능. CUSTOMER는 본인 주문만, MASTER는 모든 주문 수정 가능.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "PENDING 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 없음")
    })
    @PatchMapping("/{orderId}/request")
    public ApiResponse<ResOrderDto> updateRequest(
            @Parameter(description = "주문 ID") @PathVariable UUID orderId,
            @RequestBody @Valid ReqUpdateRequestDto request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return switch (principal.role()) {
            case CUSTOMER -> ApiResponse.ok(
                    orderService.updateRequestByCustomer(orderId, principal.username(), request.request()));
            case MASTER -> ApiResponse.ok(orderService.updateRequestByMaster(orderId, request.request()));
            default -> throw new BusinessException(ErrorCode.FORBIDDEN);
        };
    }

    /** 특정 단일 role만 허용. 위반 시 403. */
    private void requireRole(UserPrincipal principal, UserRole expected) {
        if (principal.role() != expected) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
