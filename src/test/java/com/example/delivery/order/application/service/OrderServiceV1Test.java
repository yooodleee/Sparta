package com.example.delivery.order.application.service;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.order.domain.entity.OrderEntity;
import com.example.delivery.order.domain.entity.OrderStatus;
import com.example.delivery.order.domain.repository.OrderRepository;
import com.example.delivery.order.presentation.dto.request.ReqCreateOrderDto;
import com.example.delivery.order.presentation.dto.request.ReqOrderItemDto;
import com.example.delivery.order.presentation.dto.response.ResOrderDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderServiceV1Test {

    @Mock OrderRepository orderRepository;

    @InjectMocks OrderServiceV1 orderService;

    // ─── 공통 픽스처 ───────────────────────────────────────────
    private static final String CUSTOMER_ID = "cust01";
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID ADDRESS_ID = UUID.randomUUID();

    private ReqCreateOrderDto validCreateDto() {
        return new ReqCreateOrderDto(
                STORE_ID,
                ADDRESS_ID,
                "문 앞에 두고 가주세요.",
                List.of(
                        new ReqOrderItemDto(UUID.randomUUID(), 2, 18_000),
                        new ReqOrderItemDto(UUID.randomUUID(), 1, 4_000)
                )
        );
    }

    private OrderEntity buildOrder() {
        OrderEntity order = OrderEntity.builder()
                .customerId(CUSTOMER_ID)
                .storeId(STORE_ID)
                .addressId(ADDRESS_ID)
                .totalPrice(40_000)
                .request("요청")
                .build();
        ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.now());
        return order;
    }

    private void setStatus(OrderEntity order, OrderStatus status) {
        ReflectionTestUtils.setField(order, "status", status);
    }

    // ─── createOrder ────────────────────────────────────────
    @Nested
    @DisplayName("createOrder — 주문 생성")
    class CreateOrder {

        @Test
        @DisplayName("정상 생성 시 totalPrice가 항목별 합계로 계산되어 저장된다")
        void success_calculatesTotalPrice() {
            // given
            given(orderRepository.save(any(OrderEntity.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // when
            ResOrderDto result = orderService.createOrder(CUSTOMER_ID, validCreateDto());

            // then
            assertThat(result.customerId()).isEqualTo(CUSTOMER_ID);
            assertThat(result.storeId()).isEqualTo(STORE_ID);
            assertThat(result.addressId()).isEqualTo(ADDRESS_ID);
            assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
            assertThat(result.totalPrice()).isEqualTo(2 * 18_000 + 1 * 4_000);
            assertThat(result.items()).hasSize(2);
        }
    }

    // ─── getOrder ────────────────────────────────────────────
    @Nested
    @DisplayName("getOrder — 주문 단건 조회")
    class GetOrder {

        @Test
        @DisplayName("존재하는 주문 ID → 정상 반환")
        void found_returnsDto() {
            // given
            UUID orderId = UUID.randomUUID();
            OrderEntity order = buildOrder();
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            ResOrderDto result = orderService.getOrder(orderId);

            // then
            assertThat(result.customerId()).isEqualTo(CUSTOMER_ID);
        }

        @Test
        @DisplayName("존재하지 않는 주문 ID → ORDER_NOT_FOUND 예외")
        void notFound_throwsException() {
            // given
            UUID orderId = UUID.randomUUID();
            given(orderRepository.findById(orderId)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> orderService.getOrder(orderId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_NOT_FOUND));
        }
    }

    // ─── cancelByCustomer ───────────────────────────────────
    @Nested
    @DisplayName("cancelByCustomer — 고객 주문 취소")
    class CancelByCustomer {

        @Test
        @DisplayName("PENDING + 5분 이내 → CANCELLED 전이")
        void pendingWithinLimit_cancels() {
            // given
            UUID orderId = UUID.randomUUID();
            OrderEntity order = buildOrder();
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            ResOrderDto result = orderService.cancelByCustomer(orderId);

            // then
            assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("PENDING이 아니면 INVALID_ORDER_STATUS 예외")
        void notPending_throwsException() {
            // given
            UUID orderId = UUID.randomUUID();
            OrderEntity order = buildOrder();
            setStatus(order, OrderStatus.ACCEPTED);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when / then
            assertThatThrownBy(() -> orderService.cancelByCustomer(orderId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_ORDER_STATUS));
        }

        @Test
        @DisplayName("생성 후 5분이 지나면 ORDER_CANCEL_TIMEOUT 예외")
        void overTimeLimit_throwsException() {
            // given
            UUID orderId = UUID.randomUUID();
            OrderEntity order = buildOrder();
            ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.now().minusMinutes(10));
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when / then
            assertThatThrownBy(() -> orderService.cancelByCustomer(orderId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_CANCEL_TIMEOUT));
        }
    }

    // ─── cancelByMaster ─────────────────────────────────────
    @Nested
    @DisplayName("cancelByMaster — MASTER 주문 취소")
    class CancelByMaster {

        @Test
        @DisplayName("어떤 상태든 CANCELLED로 전이")
        void anyStatus_cancels() {
            // given
            UUID orderId = UUID.randomUUID();
            OrderEntity order = buildOrder();
            setStatus(order, OrderStatus.COOKING);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            ResOrderDto result = orderService.cancelByMaster(orderId);

            // then
            assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        }
    }

    // ─── changeStatusByOwner ─────────────────────────────────
    @Nested
    @DisplayName("changeStatusByOwner — OWNER 상태 변경")
    class ChangeStatusByOwner {

        @Test
        @DisplayName("PENDING → ACCEPTED 정상 전이")
        void forwardTransition_success() {
            // given
            UUID orderId = UUID.randomUUID();
            OrderEntity order = buildOrder();
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            ResOrderDto result = orderService.changeStatusByOwner(orderId, OrderStatus.ACCEPTED);

            // then
            assertThat(result.status()).isEqualTo(OrderStatus.ACCEPTED);
        }

        @Test
        @DisplayName("PENDING → COOKING 같은 역방향/건너뛰기 전이는 INVALID_ORDER_STATUS 예외")
        void invalidTransition_throwsException() {
            // given
            UUID orderId = UUID.randomUUID();
            OrderEntity order = buildOrder();
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when / then
            assertThatThrownBy(() -> orderService.changeStatusByOwner(orderId, OrderStatus.COOKING))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_ORDER_STATUS));
        }

        @Test
        @DisplayName("OWNER는 CANCELLED 전이 불가 → INVALID_ORDER_STATUS 예외")
        void cancelTransition_throwsException() {
            // given
            UUID orderId = UUID.randomUUID();
            OrderEntity order = buildOrder();
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when / then
            assertThatThrownBy(() -> orderService.changeStatusByOwner(orderId, OrderStatus.CANCELLED))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_ORDER_STATUS));
        }
    }

    // ─── changeStatusByManager ───────────────────────────────
    @Nested
    @DisplayName("changeStatusByManager — MANAGER 상태 변경")
    class ChangeStatusByManager {

        @Test
        @DisplayName("일반 상태 전이는 성공")
        void anyNonCancel_success() {
            // given
            UUID orderId = UUID.randomUUID();
            OrderEntity order = buildOrder();
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            ResOrderDto result = orderService.changeStatusByManager(orderId, OrderStatus.DELIVERING);

            // then
            assertThat(result.status()).isEqualTo(OrderStatus.DELIVERING);
        }

        @Test
        @DisplayName("MANAGER는 CANCELLED 전이 불가 → FORBIDDEN 예외")
        void cancel_throwsForbidden() {
            // given
            UUID orderId = UUID.randomUUID();
            OrderEntity order = buildOrder();
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when / then
            assertThatThrownBy(() -> orderService.changeStatusByManager(orderId, OrderStatus.CANCELLED))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.FORBIDDEN));
        }
    }

    // ─── changeStatusByMaster ────────────────────────────────
    @Nested
    @DisplayName("changeStatusByMaster — MASTER 상태 변경")
    class ChangeStatusByMaster {

        @Test
        @DisplayName("임의 상태로 전이 가능")
        void anyStatus_success() {
            // given
            UUID orderId = UUID.randomUUID();
            OrderEntity order = buildOrder();
            setStatus(order, OrderStatus.DELIVERED);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            ResOrderDto result = orderService.changeStatusByMaster(orderId, OrderStatus.PENDING);

            // then
            assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        }
    }

    // ─── updateRequest ───────────────────────────────────────
    @Nested
    @DisplayName("updateRequest — 요청사항 수정")
    class UpdateRequest {

        @Test
        @DisplayName("PENDING 상태에서는 수정 가능")
        void pending_success() {
            // given
            UUID orderId = UUID.randomUUID();
            OrderEntity order = buildOrder();
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            ResOrderDto result = orderService.updateRequest(orderId, "수정된 요청");

            // then
            assertThat(result.request()).isEqualTo("수정된 요청");
        }

        @Test
        @DisplayName("PENDING이 아니면 INVALID_ORDER_STATUS 예외")
        void notPending_throwsException() {
            // given
            UUID orderId = UUID.randomUUID();
            OrderEntity order = buildOrder();
            setStatus(order, OrderStatus.COOKING);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when / then
            assertThatThrownBy(() -> orderService.updateRequest(orderId, "수정"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_ORDER_STATUS));
        }
    }
}
