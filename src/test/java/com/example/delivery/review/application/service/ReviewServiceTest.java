package com.example.delivery.review.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.global.infrastructure.security.UserPrincipal;
import com.example.delivery.order.domain.entity.OrderEntity;
import com.example.delivery.order.infrastructure.repository.OrderRepository;
import com.example.delivery.review.domain.entity.ReviewEntity;
import com.example.delivery.review.domain.repository.ReviewRepository;
import com.example.delivery.review.presentation.dto.request.ReqCreateReviewDto;
import com.example.delivery.user.domain.entity.UserRole;
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
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock OrderRepository  orderRepository;

    @InjectMocks ReviewService reviewService;

    // ─── 공통 픽스처 ───────────────────────────────────────────────
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();

    private final UserPrincipal principal =
            new UserPrincipal(1L, "testuser", UserRole.CUSTOMER);

    private ReqCreateReviewDto validRequest() {
        return new ReqCreateReviewDto(5, "맛있었어요!", STORE_ID);
    }

    // ─── createReview ──────────────────────────────────────────────
    @Nested
    @DisplayName("createReview — 리뷰 생성")
    class CreateReview {

        @Test
        @DisplayName("존재하지 않는 orderId → ORDER_NOT_FOUND 예외")
        void orderNotFound_throwsException() {
            // given
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> reviewService.createReview(ORDER_ID, validRequest(), principal))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_NOT_FOUND));

            verify(reviewRepository, never()).existsByOrderId(any());
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하는 orderId, 중복 리뷰 → DUPLICATE_REVIEW 예외")
        void duplicateReview_throwsException() {
            // given
            given(orderRepository.findById(ORDER_ID))
                    .willReturn(Optional.of(mock(OrderEntity.class)));
            given(reviewRepository.existsByOrderId(ORDER_ID)).willReturn(true);

            // when / then
            assertThatThrownBy(() -> reviewService.createReview(ORDER_ID, validRequest(), principal))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_REVIEW));

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하는 orderId, 중복 없음 → 리뷰 저장")
        void success_reviewSaved() {
            // given
            ReviewEntity savedReview = ReviewEntity.builder()
                    .orderId(ORDER_ID)
                    .storeId(STORE_ID)
                    .customerId(principal.username())
                    .rating(5)
                    .content("맛있었어요!")
                    .build();

            given(orderRepository.findById(ORDER_ID))
                    .willReturn(Optional.of(mock(OrderEntity.class)));
            given(reviewRepository.existsByOrderId(ORDER_ID)).willReturn(false);
            given(reviewRepository.save(any())).willReturn(savedReview);

            // when
            reviewService.createReview(ORDER_ID, validRequest(), principal);

            // then
            verify(reviewRepository).save(any(ReviewEntity.class));
        }
    }
}