package com.example.delivery.order.presentation.controller;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.global.infrastructure.config.SecurityConfig;
import com.example.delivery.global.infrastructure.security.JwtTokenProvider;
import com.example.delivery.global.infrastructure.security.RestAccessDeniedHandler;
import com.example.delivery.global.infrastructure.security.RestAuthenticationEntryPoint;
import com.example.delivery.global.infrastructure.security.UserPrincipal;
import com.example.delivery.order.application.service.OrderServiceV1;
import com.example.delivery.order.domain.entity.OrderStatus;
import com.example.delivery.order.domain.entity.OrderType;
import com.example.delivery.order.presentation.dto.request.ReqChangeStatusDto;
import com.example.delivery.order.presentation.dto.request.ReqCreateOrderDto;
import com.example.delivery.order.presentation.dto.request.ReqOrderItemDto;
import com.example.delivery.order.presentation.dto.request.ReqUpdateRequestDto;
import com.example.delivery.order.presentation.dto.response.ResOrderDto;
import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.user.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link OrderControllerV1} WebMvc 테스트.
 *
 * [테스트 전략]
 * - {@code @WebMvcTest}로 컨트롤러 슬라이스만 로드하고, 서비스·JWT·인증 예외 핸들러는
 *   {@link MockBean}으로 대체한다 → 컨트롤러의 요청 매핑·role 분기·본인 확인 로직에만 집중.
 * - {@link SecurityConfig}는 {@code @Import}로 끌어와 실제 URL 매처/인증 필터 동작까지 검증.
 *
 * [인증 흐름 모킹]
 * - 인증 없음 → {@code RestAuthenticationEntryPoint}가 401을 반환하도록 스텁.
 * - 권한 없음 → {@code RestAccessDeniedHandler}가 403을 반환하도록 스텁.
 * - 인증 필요 테스트는 {@code SecurityMockMvcRequestPostProcessors.authentication}으로
 *   {@link UsernamePasswordAuthenticationToken}을 직접 주입 (JWT 파싱 우회).
 *
 * [role 별 픽스처]
 * CUSTOMER(본인/타인) · OWNER · MANAGER · MASTER — 컨트롤러 디스패치 분기를 모두 커버.
 */
@WebMvcTest(OrderControllerV1.class)
@Import(SecurityConfig.class)
class OrderControllerV1Test {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean OrderServiceV1 orderService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean RestAuthenticationEntryPoint authenticationEntryPoint;
    @MockBean RestAccessDeniedHandler accessDeniedHandler;
    // SecurityConfig의 JwtAuthenticationFilter가 UserRepository를 요구 — 슬라이스 테스트에선 미등록이라 직접 모킹.
    @MockBean UserRepository userRepository;

    // ─── 공통 픽스처 ─────────────────────────────────────────────────
    private static final String CUSTOMER_NAME = "cust01";
    private static final UUID ORDER_ID   = UUID.randomUUID();
    private static final UUID STORE_ID   = UUID.randomUUID();
    private static final UUID ADDRESS_ID = UUID.randomUUID();

    // 본인 확인 로직 검증을 위해 CUSTOMER는 2명(cust01, other)을 준비한다.
    private final UserPrincipal customer  = new UserPrincipal(UUID.randomUUID(), CUSTOMER_NAME, UserRole.CUSTOMER);
    private final UserPrincipal otherUser = new UserPrincipal(UUID.randomUUID(), "other",       UserRole.CUSTOMER);
    private final UserPrincipal owner     = new UserPrincipal(UUID.randomUUID(), "owner",       UserRole.OWNER);
    private final UserPrincipal manager   = new UserPrincipal(UUID.randomUUID(), "manager",     UserRole.MANAGER);
    private final UserPrincipal master    = new UserPrincipal(UUID.randomUUID(), "master",      UserRole.MASTER);

    /**
     * 인증/인가 실패 시의 HTTP 상태 코드를 테스트 스텁으로 고정.
     * (실제 엔트리포인트/핸들러 로직은 별도 테스트에서 검증.)
     */
    @BeforeEach
    void setupSecurityHandlers() throws Exception {
        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }).when(authenticationEntryPoint).commence(any(), any(), any());

        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }).when(accessDeniedHandler).handle(any(), any(), any());
    }

    /** 주어진 principal로 인증된 토큰을 구성. ROLE_ 접두사는 Spring Security 컨벤션. */
    private UsernamePasswordAuthenticationToken auth(UserPrincipal p) {
        return new UsernamePasswordAuthenticationToken(
                p, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + p.role().name())));
    }

    /** 응답 DTO 샘플 — 테스트에서 서비스 반환값을 스텁할 때 사용. */
    private ResOrderDto sampleOrder(String customerId, OrderStatus status) {
        return new ResOrderDto(
                ORDER_ID, customerId, STORE_ID, ADDRESS_ID,
                OrderType.ONLINE, status, 40_000, "요청",
                null, null, null,
                List.of()
        );
    }

    // ── createOrder ─────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/orders — 주문 생성")
    class CreateOrder {

        private ReqCreateOrderDto validRequest() {
            return new ReqCreateOrderDto(
                    STORE_ID, ADDRESS_ID, "요청사항",
                    List.of(new ReqOrderItemDto(UUID.randomUUID(), 2, 18_000))
            );
        }

        @Test
        @DisplayName("성공 — CUSTOMER 가 201 Created")
        void success() throws Exception {
            // principal.username() 이 서비스의 customerId 인자로 그대로 전달되는지 확인
            given(orderService.createOrder(eq(CUSTOMER_NAME), any()))
                    .willReturn(sampleOrder(CUSTOMER_NAME, OrderStatus.PENDING));

            mockMvc.perform(post("/api/v1/orders")
                            .with(authentication(auth(customer)))
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @DisplayName("실패 — 인증 없음 401")
        void unauthorized() throws Exception {
            // SecurityConfig의 authenticated() 매처가 걸러 authenticationEntryPoint로 위임됨
            mockMvc.perform(post("/api/v1/orders")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 — CUSTOMER 아니면 403")
        void forbidden_notCustomer() throws Exception {
            // 인증은 되어 있지만 role이 OWNER → requireRole()에서 FORBIDDEN
            mockMvc.perform(post("/api/v1/orders")
                            .with(authentication(auth(owner)))
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패 — 주문 항목 빈 배열 400")
        void invalid_emptyItems() throws Exception {
            // @NotEmpty 위반 → MethodArgumentNotValidException → 400
            ReqCreateOrderDto req = new ReqCreateOrderDto(STORE_ID, ADDRESS_ID, "요청", List.of());

            mockMvc.perform(post("/api/v1/orders")
                            .with(authentication(auth(customer)))
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── getOrder ────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/orders/{orderId} — 단건 조회")
    class GetOrder {

        @Test
        @DisplayName("성공 — 200 OK")
        void success() throws Exception {
            given(orderService.getOrder(ORDER_ID))
                    .willReturn(sampleOrder(CUSTOMER_NAME, OrderStatus.PENDING));

            // 인증 없이도 조회 가능 (SecurityConfig의 anyRequest().permitAll())
            mockMvc.perform(get("/api/v1/orders/{orderId}", ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.orderId").value(ORDER_ID.toString()));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 주문 404")
        void notFound() throws Exception {
            // 서비스가 ORDER_NOT_FOUND 던짐 → GlobalExceptionHandler가 404로 매핑
            given(orderService.getOrder(ORDER_ID))
                    .willThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

            mockMvc.perform(get("/api/v1/orders/{orderId}", ORDER_ID))
                    .andExpect(status().isNotFound());
        }
    }

    // ── getOrders ───────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/orders — 목록 조회")
    class GetOrders {

        @Test
        @DisplayName("성공 — MASTER 는 customerId 필터 없이 전체 조회")
        void master_success() throws Exception {
            Page<ResOrderDto> page = new PageImpl<>(
                    List.of(sampleOrder(CUSTOMER_NAME, OrderStatus.PENDING)),
                    PageRequest.of(0, 10), 1);
            // MASTER → customerIdFilter = null
            given(orderService.getOrders(eq(null), eq(null), eq(null), any())).willReturn(page);

            mockMvc.perform(get("/api/v1/orders")
                            .with(authentication(auth(master))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("성공 — CUSTOMER 는 customerId 가 본인으로 강제 주입")
        void customer_filtersByOwnUsername() throws Exception {
            Page<ResOrderDto> page = new PageImpl<>(
                    List.of(sampleOrder(CUSTOMER_NAME, OrderStatus.PENDING)),
                    PageRequest.of(0, 10), 1);
            given(orderService.getOrders(eq(CUSTOMER_NAME), eq(null), eq(null), any())).willReturn(page);

            mockMvc.perform(get("/api/v1/orders")
                            .with(authentication(auth(customer))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("실패 — 인증 없으면 403")
        void unauthenticated_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/orders"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── cancelOrder ─────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/orders/{orderId}/cancel — 주문 취소")
    class CancelOrder {

        @Test
        @DisplayName("성공 — CUSTOMER 본인")
        void customerOwner_success() throws Exception {
            // principal.username() 이 서비스 인자로 그대로 전달되고, 엔티티가 본인 확인을 수행
            given(orderService.cancelByCustomer(ORDER_ID, CUSTOMER_NAME))
                    .willReturn(sampleOrder(CUSTOMER_NAME, OrderStatus.CANCELLED));

            mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", ORDER_ID)
                            .with(authentication(auth(customer)))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("실패 — CUSTOMER 타인 주문 403")
        void customerOther_forbidden() throws Exception {
            // 요청자는 other → 엔티티의 verifyCustomer 가 FORBIDDEN 을 던짐
            given(orderService.cancelByCustomer(ORDER_ID, "other"))
                    .willThrow(new BusinessException(ErrorCode.FORBIDDEN));

            mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", ORDER_ID)
                            .with(authentication(auth(otherUser)))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("성공 — MASTER 는 타인 주문도 취소")
        void master_success() throws Exception {
            // MASTER는 verifyOwner를 거치지 않고 cancelByMaster로 바로 진입
            given(orderService.cancelByMaster(ORDER_ID))
                    .willReturn(sampleOrder(CUSTOMER_NAME, OrderStatus.CANCELLED));

            mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", ORDER_ID)
                            .with(authentication(auth(master)))
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("실패 — OWNER/MANAGER 는 취소 불가 403")
        void ownerOrManager_forbidden() throws Exception {
            // switch 의 default 분기로 들어가 FORBIDDEN
            mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", ORDER_ID)
                            .with(authentication(auth(owner)))
                            .with(csrf()))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", ORDER_ID)
                            .with(authentication(auth(manager)))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패 — 인증 없음 401")
        void unauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", ORDER_ID)
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 — CUSTOMER 5분 초과 시 400")
        void customer_timeout() throws Exception {
            // 본인 확인은 통과하지만 엔티티에서 ORDER_CANCEL_TIMEOUT 발생
            given(orderService.cancelByCustomer(ORDER_ID, CUSTOMER_NAME))
                    .willThrow(new BusinessException(ErrorCode.ORDER_CANCEL_TIMEOUT));

            mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", ORDER_ID)
                            .with(authentication(auth(customer)))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── changeStatus ────────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/v1/orders/{orderId}/status — 상태 변경")
    class ChangeStatus {

        /** JSON 본문 생성 헬퍼 — 테스트마다 status만 바꿔서 재사용. */
        private String body(OrderStatus status) throws Exception {
            return objectMapper.writeValueAsString(new ReqChangeStatusDto(status));
        }

        @Test
        @DisplayName("성공 — OWNER 정방향")
        void owner_success() throws Exception {
            // OWNER → changeStatusByOwner 로 디스패치 확인
            given(orderService.changeStatusByOwner(ORDER_ID, OrderStatus.ACCEPTED))
                    .willReturn(sampleOrder(CUSTOMER_NAME, OrderStatus.ACCEPTED));

            mockMvc.perform(patch("/api/v1/orders/{orderId}/status", ORDER_ID)
                            .with(authentication(auth(owner)))
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(body(OrderStatus.ACCEPTED)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
        }

        @Test
        @DisplayName("실패 — OWNER 역방향 전이 400")
        void owner_invalidTransition() throws Exception {
            // 엔티티의 canTransitionTo가 false → INVALID_ORDER_STATUS
            given(orderService.changeStatusByOwner(eq(ORDER_ID), any()))
                    .willThrow(new BusinessException(ErrorCode.INVALID_ORDER_STATUS));

            mockMvc.perform(patch("/api/v1/orders/{orderId}/status", ORDER_ID)
                            .with(authentication(auth(owner)))
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(body(OrderStatus.COOKING)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("성공 — MANAGER CANCELLED 외 전이")
        void manager_success() throws Exception {
            given(orderService.changeStatusByManager(ORDER_ID, OrderStatus.DELIVERING))
                    .willReturn(sampleOrder(CUSTOMER_NAME, OrderStatus.DELIVERING));

            mockMvc.perform(patch("/api/v1/orders/{orderId}/status", ORDER_ID)
                            .with(authentication(auth(manager)))
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(body(OrderStatus.DELIVERING)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("성공 — MASTER 임의 전이")
        void master_success() throws Exception {
            // MASTER는 제약 없으므로 역방향(DELIVERED → PENDING)도 허용
            given(orderService.changeStatusByMaster(ORDER_ID, OrderStatus.PENDING))
                    .willReturn(sampleOrder(CUSTOMER_NAME, OrderStatus.PENDING));

            mockMvc.perform(patch("/api/v1/orders/{orderId}/status", ORDER_ID)
                            .with(authentication(auth(master)))
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(body(OrderStatus.PENDING)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("실패 — CUSTOMER 는 상태 변경 불가 403")
        void customer_forbidden() throws Exception {
            // switch default 분기 → FORBIDDEN (서비스 호출 자체가 없음)
            mockMvc.perform(patch("/api/v1/orders/{orderId}/status", ORDER_ID)
                            .with(authentication(auth(customer)))
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(body(OrderStatus.ACCEPTED)))
                    .andExpect(status().isForbidden());
        }
    }

    // ── updateRequest ───────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/v1/orders/{orderId}/request — 요청사항 수정")
    class UpdateRequest {

        private String body(String request) throws Exception {
            return objectMapper.writeValueAsString(new ReqUpdateRequestDto(request));
        }

        @Test
        @DisplayName("성공 — CUSTOMER 본인")
        void customerOwner_success() throws Exception {
            given(orderService.updateRequestByCustomer(ORDER_ID, CUSTOMER_NAME, "수정"))
                    .willReturn(sampleOrder(CUSTOMER_NAME, OrderStatus.PENDING));

            mockMvc.perform(patch("/api/v1/orders/{orderId}/request", ORDER_ID)
                            .with(authentication(auth(customer)))
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(body("수정")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("실패 — CUSTOMER 타인 주문 403")
        void customerOther_forbidden() throws Exception {
            // 엔티티의 verifyCustomer 가 FORBIDDEN 을 던짐
            given(orderService.updateRequestByCustomer(ORDER_ID, "other", "수정"))
                    .willThrow(new BusinessException(ErrorCode.FORBIDDEN));

            mockMvc.perform(patch("/api/v1/orders/{orderId}/request", ORDER_ID)
                            .with(authentication(auth(otherUser)))
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(body("수정")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("성공 — MASTER 는 타인 주문도 수정")
        void master_success() throws Exception {
            // MASTER는 본인 확인을 생략하는 전용 서비스 메서드로 바로 진입
            given(orderService.updateRequestByMaster(ORDER_ID, "수정"))
                    .willReturn(sampleOrder(CUSTOMER_NAME, OrderStatus.PENDING));

            mockMvc.perform(patch("/api/v1/orders/{orderId}/request", ORDER_ID)
                            .with(authentication(auth(master)))
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(body("수정")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("실패 — OWNER/MANAGER 는 수정 불가 403")
        void ownerOrManager_forbidden() throws Exception {
            // CUSTOMER/MASTER 외 전부 FORBIDDEN
            mockMvc.perform(patch("/api/v1/orders/{orderId}/request", ORDER_ID)
                            .with(authentication(auth(owner)))
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(body("수정")))
                    .andExpect(status().isForbidden());

            mockMvc.perform(patch("/api/v1/orders/{orderId}/request", ORDER_ID)
                            .with(authentication(auth(manager)))
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(body("수정")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패 — PENDING 아니면 400")
        void notPending() throws Exception {
            // 엔티티 레벨에서 PENDING 아님 → INVALID_ORDER_STATUS
            given(orderService.updateRequestByCustomer(ORDER_ID, CUSTOMER_NAME, "수정"))
                    .willThrow(new BusinessException(ErrorCode.INVALID_ORDER_STATUS));

            mockMvc.perform(patch("/api/v1/orders/{orderId}/request", ORDER_ID)
                            .with(authentication(auth(customer)))
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(body("수정")))
                    .andExpect(status().isBadRequest());
        }
    }
}
