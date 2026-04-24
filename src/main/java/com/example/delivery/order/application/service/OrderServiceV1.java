package com.example.delivery.order.application.service;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.order.domain.entity.OrderEntity;
import com.example.delivery.order.domain.entity.OrderItemEntity;
import com.example.delivery.order.domain.entity.OrderStatus;
import com.example.delivery.order.domain.repository.OrderRepository;
import com.example.delivery.order.presentation.dto.request.ReqCreateOrderDto;
import com.example.delivery.order.presentation.dto.request.ReqOrderItemDto;
import com.example.delivery.order.presentation.dto.response.ResOrderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * 주문 서비스 V1.
 *
 * [의존성 흐름]
 * OrderControllerV1 → OrderServiceV1 → OrderRepository (도메인 인터페이스)
 *
 * [상태 전이 정책 — docs/02-domain-spec.md §3.2]
 * PENDING → ACCEPTED → COOKING → DELIVERING → DELIVERED → COMPLETED
 *   - CUSTOMER: 생성(PENDING) + 5분 이내 취소만 가능
 *   - OWNER   : 위 순서대로만 전이 가능 (역방향/CANCELLED 불가)
 *   - MANAGER : CANCELLED 외 자유 전이
 *   - MASTER  : 모든 상태 전이 가능 (취소 포함)
 *
 * [도메인 로직 위임]
 * 상태 전이·불변식 검증은 OrderEntity가 스스로 수행한다.
 * 서비스는 트랜잭션 경계 + Repository 조회 + 엔티티 메서드 오케스트레이션만 담당.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceV1 {

    private final OrderRepository orderRepository;

    /**
     * 주문 생성 — CUSTOMER.
     *
     * 1. 항목별 (quantity × unitPrice) 합산으로 totalPrice 계산 — 주문 시점 스냅샷
     * 2. OrderEntity 생성 (status=PENDING 기본값)
     * 3. 각 OrderItemEntity를 추가 (Cascade ALL로 함께 저장)
     * 4. 저장 후 응답 DTO 변환
     */
    @Transactional
    public ResOrderDto createOrder(String customerId, ReqCreateOrderDto dto) {
        int totalPrice = dto.items().stream()
                .mapToInt(item -> item.quantity() * item.unitPrice())
                .sum();

        OrderEntity order = OrderEntity.builder()
                .customerId(customerId)
                .storeId(dto.storeId())
                .addressId(dto.addressId())
                .totalPrice(totalPrice)
                .request(dto.request())
                .build();

        for (ReqOrderItemDto item : dto.items()) {
            order.addItem(OrderItemEntity.builder()
                    .menuId(item.menuId())
                    .quantity(item.quantity())
                    .unitPrice(item.unitPrice())
                    .build());
        }

        return ResOrderDto.from(orderRepository.save(order));
    }

    /** 단건 조회 — 없으면 ORDER_NOT_FOUND. */
    public ResOrderDto getOrder(UUID orderId) {
        return ResOrderDto.from(findOrder(orderId));
    }

    /** 목록 조회 — 페이지네이션 지원. */
    public Page<ResOrderDto> getOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(ResOrderDto::from);
    }

    /**
     * CUSTOMER 주문 취소.
     * 본인 여부 + PENDING 상태 + 생성 후 5분 이내 조건을 OrderEntity가 검증한다.
     * Clock을 주입하는 이유는 단위 테스트에서 시간을 고정하기 위함 (현재는 시스템 시계 사용).
     */
    @Transactional
    public ResOrderDto cancelByCustomer(UUID orderId, String customerId) {
        OrderEntity order = findOrder(orderId);
        order.cancelByCustomer(customerId, Clock.systemDefaultZone());
        return ResOrderDto.from(order);
    }

    /** MASTER 주문 취소 — 상태/시간 제약 없이 강제 CANCELLED 전이. */
    @Transactional
    public ResOrderDto cancelByMaster(UUID orderId) {
        OrderEntity order = findOrder(orderId);
        order.cancelByMaster();
        return ResOrderDto.from(order);
    }

    /**
     * OWNER 상태 변경 — 정방향 전이만 허용.
     * 역방향/건너뛰기/CANCELLED 전이는 OrderEntity가 INVALID_ORDER_STATUS를 던진다.
     */
    @Transactional
    public ResOrderDto changeStatusByOwner(UUID orderId, OrderStatus next) {
        OrderEntity order = findOrder(orderId);
        order.changeStatusByOwner(next);
        return ResOrderDto.from(order);
    }

    /** MANAGER 상태 변경 — CANCELLED만 거부 (FORBIDDEN), 나머지는 자유. */
    @Transactional
    public ResOrderDto changeStatusByManager(UUID orderId, OrderStatus next) {
        OrderEntity order = findOrder(orderId);
        order.changeStatusByManager(next);
        return ResOrderDto.from(order);
    }

    /** MASTER 상태 변경 — 제약 없음. */
    @Transactional
    public ResOrderDto changeStatusByMaster(UUID orderId, OrderStatus next) {
        OrderEntity order = findOrder(orderId);
        order.changeStatusByMaster(next);
        return ResOrderDto.from(order);
    }

    /**
     * CUSTOMER 요청사항 수정.
     * 본인 여부 + PENDING 상태 조건을 OrderEntity가 검증한다.
     */
    @Transactional
    public ResOrderDto updateRequestByCustomer(UUID orderId, String customerId, String request) {
        OrderEntity order = findOrder(orderId);
        order.updateRequestByCustomer(customerId, request);
        return ResOrderDto.from(order);
    }

    /**
     * MASTER 요청사항 수정.
     * PENDING 상태 조건만 검증, 본인 확인은 생략.
     */
    @Transactional
    public ResOrderDto updateRequestByMaster(UUID orderId, String request) {
        OrderEntity order = findOrder(orderId);
        order.updateRequest(request);
        return ResOrderDto.from(order);
    }

    private OrderEntity findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }
}
