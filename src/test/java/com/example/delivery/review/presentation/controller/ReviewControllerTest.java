package com.example.delivery.review.presentation.controller;

import com.example.delivery.global.infrastructure.security.JwtTokenProvider;
import com.example.delivery.global.infrastructure.security.RestAccessDeniedHandler;
import com.example.delivery.global.infrastructure.security.RestAuthenticationEntryPoint;
import com.example.delivery.global.infrastructure.security.UserPrincipal;
import com.example.delivery.review.application.service.ReviewService;
import com.example.delivery.review.presentation.dto.request.ReqCreateReviewDto;
import com.example.delivery.review.presentation.dto.request.ReqUpdateReviewDto;
import com.example.delivery.review.presentation.dto.response.ResReviewDto;
import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import com.example.delivery.global.infrastructure.config.SecurityConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
class ReviewControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ReviewService reviewService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean RestAuthenticationEntryPoint authenticationEntryPoint;
    @MockBean RestAccessDeniedHandler accessDeniedHandler;
    @MockBean com.example.delivery.user.domain.repository.UserRepository userRepository;

    private static final UUID ORDER_ID    = UUID.randomUUID();
    private static final UUID STORE_ID    = UUID.randomUUID();
    private static final UUID REVIEW_ID   = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MASTER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final UserPrincipal principal =
            new UserPrincipal(CUSTOMER_ID, "testuser", UserRole.CUSTOMER);

    @BeforeEach
    void setupSecurityHandlers() throws Exception {
        // 인증 없을 때 401 반환하도록 설정
        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }).when(authenticationEntryPoint).commence(any(), any(), any());

        // 권한 없을 때 403 반환하도록 설정
        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }).when(accessDeniedHandler).handle(any(), any(), any());
    }

    private UsernamePasswordAuthenticationToken auth(UserPrincipal p) {
        return new UsernamePasswordAuthenticationToken(
                p, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + p.role().name())));
    }

    private ResReviewDto sampleDto() {
        return new ResReviewDto(
                REVIEW_ID, ORDER_ID, STORE_ID,
                "임시 가게명", "testuser", "임시 닉네임",
                5, "맛있었어요!", LocalDateTime.now(), LocalDateTime.now());
    }

    // ── createReview ─────────────────────────────────────────────

    @Test
    @DisplayName("리뷰 생성 - 성공")
    void createReview_success() throws Exception {
        ReqCreateReviewDto req = new ReqCreateReviewDto(5, "맛있었어요!", STORE_ID);
        given(reviewService.createReview(eq(ORDER_ID), any(), any()))
                .willReturn(sampleDto());

        mockMvc.perform(post("/api/v1/orders/{orderId}/reviews", ORDER_ID)
                        .with(authentication(auth(principal)))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.rating").value(5));
    }

    @Test
    @DisplayName("리뷰 생성 - 실패: 인증 없음 (401)")
    void createReview_unauthorized() throws Exception {
        ReqCreateReviewDto req = new ReqCreateReviewDto(5, "맛있었어요!", STORE_ID);

        mockMvc.perform(post("/api/v1/orders/{orderId}/reviews", ORDER_ID)
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("리뷰 생성 - 실패: 중복 리뷰 (409)")
    void createReview_duplicate() throws Exception {
        ReqCreateReviewDto req = new ReqCreateReviewDto(5, "맛있었어요!", STORE_ID);
        given(reviewService.createReview(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.DUPLICATE_REVIEW));

        mockMvc.perform(post("/api/v1/orders/{orderId}/reviews", ORDER_ID)
                        .with(authentication(auth(principal)))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("리뷰 생성 - 실패: rating 범위 초과 (400)")
    void createReview_invalidRating() throws Exception {
        String reqBody = """
                {"rating": 6, "storeId": "%s"}
                """.formatted(STORE_ID);

        mockMvc.perform(post("/api/v1/orders/{orderId}/reviews", ORDER_ID)
                        .with(authentication(auth(principal)))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("리뷰 생성 - 실패: storeId 누락 (400)")
    void createReview_missingStoreId() throws Exception {
        String reqBody = """
                {"rating": 5}
                """;

        mockMvc.perform(post("/api/v1/orders/{orderId}/reviews", ORDER_ID)
                        .with(authentication(auth(principal)))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isBadRequest());
    }

    // ── getReviewsByStore ─────────────────────────────────────────

    @Test
    @DisplayName("가게 리뷰 목록 조회 - 성공 (인증 불필요)")
    void getReviewsByStore_success() throws Exception {
        Page<ResReviewDto> page = new PageImpl<>(
                List.of(sampleDto()), PageRequest.of(0, 10), 1);
        given(reviewService.getReviewsByStore(eq(STORE_ID), eq(null), any()))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/stores/{storeId}/reviews", STORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("가게 리뷰 목록 조회 - 평점 필터")
    void getReviewsByStore_ratingFilter() throws Exception {
        Page<ResReviewDto> page = new PageImpl<>(List.of(sampleDto()), PageRequest.of(0, 10), 1);
        given(reviewService.getReviewsByStore(eq(STORE_ID), eq(5), any()))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/stores/{storeId}/reviews", STORE_ID)
                        .param("rating", "5"))
                .andExpect(status().isOk());
    }

    // ── updateReview ──────────────────────────────────────────────

    @Test
    @DisplayName("리뷰 수정 - 성공")
    void updateReview_success() throws Exception {
        ReqUpdateReviewDto req = new ReqUpdateReviewDto(3, "다시 생각해보니 보통이었어요");
        given(reviewService.updateReview(eq(REVIEW_ID), any(), any()))
                .willReturn(sampleDto());

        mockMvc.perform(patch("/api/v1/reviews/{reviewId}", REVIEW_ID)
                        .with(authentication(auth(principal)))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("리뷰 수정 - 실패: 타인 리뷰 (403)")
    void updateReview_forbidden() throws Exception {
        ReqUpdateReviewDto req = new ReqUpdateReviewDto(3, "수정");
        given(reviewService.updateReview(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(patch("/api/v1/reviews/{reviewId}", REVIEW_ID)
                        .with(authentication(auth(principal)))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("리뷰 수정 - 실패: 인증 없음 (401)")
    void updateReview_unauthorized() throws Exception {
        ReqUpdateReviewDto req = new ReqUpdateReviewDto(3, "수정");

        mockMvc.perform(patch("/api/v1/reviews/{reviewId}", REVIEW_ID)
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("리뷰 수정 - 실패: 존재하지 않는 리뷰 (404)")
    void updateReview_notFound() throws Exception {
        ReqUpdateReviewDto req = new ReqUpdateReviewDto(3, "수정");
        given(reviewService.updateReview(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/reviews/{reviewId}", REVIEW_ID)
                        .with(authentication(auth(principal)))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // ── deleteReview ──────────────────────────────────────────────

    @Test
    @DisplayName("리뷰 삭제 - 성공")
    void deleteReview_success() throws Exception {
        willDoNothing().given(reviewService).deleteReview(eq(REVIEW_ID), any());

        mockMvc.perform(delete("/api/v1/reviews/{reviewId}", REVIEW_ID)
                        .with(authentication(auth(principal)))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("리뷰 삭제 - 실패: 인증 없음 (401)")
    void deleteReview_unauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/reviews/{reviewId}", REVIEW_ID)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("리뷰 삭제 - 실패: 타인 리뷰 CUSTOMER 권한 (403)")
    void deleteReview_forbidden() throws Exception {
        willThrow(new BusinessException(ErrorCode.FORBIDDEN))
                .given(reviewService).deleteReview(any(), any());

        mockMvc.perform(delete("/api/v1/reviews/{reviewId}", REVIEW_ID)
                        .with(authentication(auth(principal)))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("리뷰 삭제 - 성공: MASTER 권한으로 타인 리뷰 삭제")
    void deleteReview_masterCanDeleteAny() throws Exception {
        UserPrincipal masterPrincipal = new UserPrincipal(MASTER_ID, "master", UserRole.MASTER);
        willDoNothing().given(reviewService).deleteReview(eq(REVIEW_ID), any());

        mockMvc.perform(delete("/api/v1/reviews/{reviewId}", REVIEW_ID)
                        .with(authentication(auth(masterPrincipal)))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}