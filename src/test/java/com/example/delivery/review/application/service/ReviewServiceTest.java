package com.example.delivery.review.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.global.infrastructure.security.UserPrincipal;
import com.example.delivery.order.domain.entity.OrderEntity;
import com.example.delivery.order.domain.entity.OrderStatus;
import com.example.delivery.order.domain.repository.OrderRepository;
import com.example.delivery.review.domain.entity.ReviewEntity;
import com.example.delivery.review.domain.repository.ReviewRepository;
import com.example.delivery.review.presentation.dto.request.ReqCreateReviewDto;
import com.example.delivery.review.presentation.dto.request.ReqUpdateReviewDto;
import com.example.delivery.review.presentation.dto.response.ResReviewDto;
import com.example.delivery.store.domain.entity.StoreEntity;
import com.example.delivery.store.domain.repository.StoreRepository;
import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.user.domain.repository.UserRepository;
import com.example.delivery.user.domain.vo.Email;
import com.example.delivery.user.domain.vo.Username;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    @Mock StoreRepository  storeRepository;
    @Mock UserRepository   userRepository;

    @InjectMocks ReviewService reviewService;

    private static final UUID ORDER_ID  = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();
    private static final UUID REVIEW_ID = UUID.randomUUID();

    private final UserPrincipal principal =
            new UserPrincipal(UUID.randomUUID(), "testuser", UserRole.CUSTOMER);

    private ReqCreateReviewDto validRequest() {
        return new ReqCreateReviewDto(5, "맛있었어요!");
    }

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
        @DisplayName("주문 상태가 COMPLETED 아님 → ORDER_NOT_COMPLETED 예외")
        void orderNotCompleted_throwsException() {
            // given
            OrderEntity order = mock(OrderEntity.class);
            given(order.getStatus()).willReturn(OrderStatus.DELIVERED);
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));

            // when / then
            assertThatThrownBy(() -> reviewService.createReview(ORDER_ID, validRequest(), principal))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_NOT_COMPLETED));

            verify(reviewRepository, never()).existsByOrderId(any());
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("본인 주문이 아님 → FORBIDDEN 예외")
        void notOwnerOrder_throwsException() {
            // given
            OrderEntity order = mock(OrderEntity.class);
            given(order.getStatus()).willReturn(OrderStatus.COMPLETED);
            given(order.getCustomerId()).willReturn("otheruser");
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));

            // when / then
            assertThatThrownBy(() -> reviewService.createReview(ORDER_ID, validRequest(), principal))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.FORBIDDEN));

            verify(reviewRepository, never()).existsByOrderId(any());
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하는 orderId, 중복 리뷰 → DUPLICATE_REVIEW 예외")
        void duplicateReview_throwsException() {
            // given
            OrderEntity order = mock(OrderEntity.class);
            given(order.getStatus()).willReturn(OrderStatus.COMPLETED);
            given(order.getCustomerId()).willReturn(principal.username());
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
            given(reviewRepository.existsByOrderId(ORDER_ID)).willReturn(true);

            // when / then
            assertThatThrownBy(() -> reviewService.createReview(ORDER_ID, validRequest(), principal))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_REVIEW));

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하는 orderId, 중복 없음 → 리뷰 저장 및 가게 평균 평점 5.0 반영")
        void success_reviewSaved() {
            // given
            OrderEntity order = mock(OrderEntity.class);
            given(order.getStatus()).willReturn(OrderStatus.COMPLETED);
            given(order.getCustomerId()).willReturn(principal.username());
            given(order.getStoreId()).willReturn(STORE_ID);

            ReviewEntity savedReview = ReviewEntity.builder()
                    .orderId(ORDER_ID)
                    .storeId(STORE_ID)
                    .customerId(principal.username())
                    .rating(5)
                    .content("맛있었어요!")
                    .build();

            StoreEntity store = StoreEntity.builder()
                    .ownerId(UUID.randomUUID())
                    .categoryId(UUID.randomUUID())
                    .areaId(UUID.randomUUID())
                    .name("테스트 가게")
                    .address("서울시 강남구")
                    .phone("010-0000-0000")
                    .build();

            UserEntity customer = UserEntity.register(
                    new Username(principal.username()), "테스터",
                    new Email("tester@test.com"), "hash", UserRole.CUSTOMER);

            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
            given(reviewRepository.existsByOrderId(ORDER_ID)).willReturn(false);
            given(reviewRepository.save(any())).willReturn(savedReview);
            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));
            given(reviewRepository.findRatingsByStoreId(STORE_ID)).willReturn(List.of(5));
            given(userRepository.findByUsername(principal.username())).willReturn(Optional.of(customer));

            // when
            reviewService.createReview(ORDER_ID, validRequest(), principal);

            // then
            verify(reviewRepository).save(any(ReviewEntity.class));
            verify(storeRepository).save(store);
            assertThat(store.getAverageRating()).isEqualByComparingTo("5.0");
        }
    }

    @Nested
    @DisplayName("updateReview — 리뷰 수정")
    class UpdateReview {

        @Test
        @DisplayName("존재하지 않는 reviewId → REVIEW_NOT_FOUND 예외")
        void reviewNotFound_throwsException() {
            // given
            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> reviewService.updateReview(REVIEW_ID, new ReqUpdateReviewDto(3, "수정"), principal))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
        }

        @Test
        @DisplayName("본인 리뷰가 아님 → FORBIDDEN 예외")
        void notOwnerReview_throwsException() {
            // given
            ReviewEntity review = mock(ReviewEntity.class);
            given(review.getCustomerId()).willReturn("otheruser");
            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(review));

            // when / then
            assertThatThrownBy(() -> reviewService.updateReview(REVIEW_ID, new ReqUpdateReviewDto(3, "수정"), principal))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.FORBIDDEN));
        }

        @Test
        @DisplayName("본인 리뷰 수정 → 가게 평균 평점 3.0 반영")
        void success_recalculatesAverageRating() {
            // given
            ReviewEntity review = mock(ReviewEntity.class);
            given(review.getCustomerId()).willReturn(principal.username());
            given(review.getStoreId()).willReturn(STORE_ID);
            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(review));

            StoreEntity store = StoreEntity.builder()
                    .ownerId(UUID.randomUUID())
                    .categoryId(UUID.randomUUID())
                    .areaId(UUID.randomUUID())
                    .name("테스트 가게")
                    .address("서울시 강남구")
                    .phone("010-0000-0000")
                    .build();

            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));
            given(reviewRepository.findRatingsByStoreId(STORE_ID)).willReturn(List.of(3));

            // when
            reviewService.updateReview(REVIEW_ID, new ReqUpdateReviewDto(3, "다시 생각해보니 보통이에요"), principal);

            // then
            verify(storeRepository).save(store);
            assertThat(store.getAverageRating()).isEqualByComparingTo("3.0");
        }

        @Test
        @DisplayName("리뷰 3개 [5,3,4] → 가게 평균 평점 4.0")
        void multipleReviews_averageCalculated() {
            // given
            ReviewEntity review = mock(ReviewEntity.class);
            given(review.getCustomerId()).willReturn(principal.username());
            given(review.getStoreId()).willReturn(STORE_ID);
            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(review));

            StoreEntity store = StoreEntity.builder()
                    .ownerId(UUID.randomUUID())
                    .categoryId(UUID.randomUUID())
                    .areaId(UUID.randomUUID())
                    .name("테스트 가게")
                    .address("서울시 강남구")
                    .phone("010-0000-0000")
                    .build();

            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));
            given(reviewRepository.findRatingsByStoreId(STORE_ID)).willReturn(List.of(5, 3, 4));

            // when
            reviewService.updateReview(REVIEW_ID, new ReqUpdateReviewDto(4, "수정"), principal);

            // then
            assertThat(store.getAverageRating()).isEqualByComparingTo("4.0");
        }

        @Test
        @DisplayName("리뷰 2개 [5,4] → 가게 평균 평점 4.5")
        void twoReviews_halfPoint() {
            // given
            ReviewEntity review = mock(ReviewEntity.class);
            given(review.getCustomerId()).willReturn(principal.username());
            given(review.getStoreId()).willReturn(STORE_ID);
            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(review));

            StoreEntity store = StoreEntity.builder()
                    .ownerId(UUID.randomUUID())
                    .categoryId(UUID.randomUUID())
                    .areaId(UUID.randomUUID())
                    .name("테스트 가게")
                    .address("서울시 강남구")
                    .phone("010-0000-0000")
                    .build();

            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));
            given(reviewRepository.findRatingsByStoreId(STORE_ID)).willReturn(List.of(5, 4));

            // when
            reviewService.updateReview(REVIEW_ID, new ReqUpdateReviewDto(4, "수정"), principal);

            // then
            assertThat(store.getAverageRating()).isEqualByComparingTo("4.5");
        }

        @Test
        @DisplayName("리뷰 3개 [5,5,4] → 가게 평균 평점 4.7 (4.666... 반올림)")
        void threeReviews_roundingHalfUp() {
            // given
            ReviewEntity review = mock(ReviewEntity.class);
            given(review.getCustomerId()).willReturn(principal.username());
            given(review.getStoreId()).willReturn(STORE_ID);
            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(review));

            StoreEntity store = StoreEntity.builder()
                    .ownerId(UUID.randomUUID())
                    .categoryId(UUID.randomUUID())
                    .areaId(UUID.randomUUID())
                    .name("테스트 가게")
                    .address("서울시 강남구")
                    .phone("010-0000-0000")
                    .build();

            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));
            given(reviewRepository.findRatingsByStoreId(STORE_ID)).willReturn(List.of(5, 5, 4));

            // when
            reviewService.updateReview(REVIEW_ID, new ReqUpdateReviewDto(4, "수정"), principal);

            // then
            assertThat(store.getAverageRating()).isEqualByComparingTo("4.7");
        }
    }

    @Nested
    @DisplayName("deleteReview — 리뷰 삭제")
    class DeleteReview {

        @Test
        @DisplayName("존재하지 않는 reviewId → REVIEW_NOT_FOUND 예외")
        void reviewNotFound_throwsException() {
            // given
            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> reviewService.deleteReview(REVIEW_ID, principal))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
        }

        @Test
        @DisplayName("CUSTOMER 본인 리뷰 삭제 → 가게 평균 평점 0.0 반영")
        void success_customer_recalculatesAverageRating() {
            // given
            ReviewEntity review = mock(ReviewEntity.class);
            given(review.getCustomerId()).willReturn(principal.username());
            given(review.getStoreId()).willReturn(STORE_ID);
            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(review));

            StoreEntity store = StoreEntity.builder()
                    .ownerId(UUID.randomUUID())
                    .categoryId(UUID.randomUUID())
                    .areaId(UUID.randomUUID())
                    .name("테스트 가게")
                    .address("서울시 강남구")
                    .phone("010-0000-0000")
                    .build();

            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));
            given(reviewRepository.findRatingsByStoreId(STORE_ID)).willReturn(List.of());

            // when
            reviewService.deleteReview(REVIEW_ID, principal);

            // then
            verify(storeRepository).save(store);
            assertThat(store.getAverageRating()).isEqualByComparingTo("0.0");
        }

        @Test
        @DisplayName("CUSTOMER 타인 리뷰 삭제 시도 → FORBIDDEN 예외")
        void customer_notOwner_throwsException() {
            // given
            ReviewEntity review = mock(ReviewEntity.class);
            given(review.getCustomerId()).willReturn("otheruser");
            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(review));

            // when / then
            assertThatThrownBy(() -> reviewService.deleteReview(REVIEW_ID, principal))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.FORBIDDEN));
        }

        @Test
        @DisplayName("MASTER 타인 리뷰 삭제 → 가게 평균 평점 0.0 반영")
        void success_master_recalculatesAverageRating() {
            // given
            UserPrincipal masterPrincipal = new UserPrincipal(UUID.randomUUID(), "master", UserRole.MASTER);

            ReviewEntity review = mock(ReviewEntity.class);
            given(review.getStoreId()).willReturn(STORE_ID);
            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(review));

            StoreEntity store = StoreEntity.builder()
                    .ownerId(UUID.randomUUID())
                    .categoryId(UUID.randomUUID())
                    .areaId(UUID.randomUUID())
                    .name("테스트 가게")
                    .address("서울시 강남구")
                    .phone("010-0000-0000")
                    .build();

            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));
            given(reviewRepository.findRatingsByStoreId(STORE_ID)).willReturn(List.of());

            // when
            reviewService.deleteReview(REVIEW_ID, masterPrincipal);

            // then
            verify(storeRepository).save(store);
            assertThat(store.getAverageRating()).isEqualByComparingTo("0.0");
        }
    }

    @Nested
    @DisplayName("getReview — 리뷰 단건 조회")
    class GetReview {

        @Test
        @DisplayName("존재하지 않는 reviewId → REVIEW_NOT_FOUND 예외")
        void reviewNotFound_throwsException() {
            // given
            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> reviewService.getReview(REVIEW_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
        }

        @Test
        @DisplayName("정상 조회 → 리뷰 + 가게명 반환")
        void success_returnsReviewDto() {
            // given
            ReviewEntity review = mock(ReviewEntity.class);
            given(review.getStoreId()).willReturn(STORE_ID);
            given(review.getCustomerNickname()).willReturn("테스터");
            given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(review));

            StoreEntity store = StoreEntity.builder()
                    .ownerId(UUID.randomUUID())
                    .categoryId(UUID.randomUUID())
                    .areaId(UUID.randomUUID())
                    .name("테스트 가게")
                    .address("서울시 강남구")
                    .phone("010-0000-0000")
                    .build();
            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));

            // when
            ResReviewDto result = reviewService.getReview(REVIEW_ID);

            // then
            assertThat(result.storeName()).isEqualTo("테스트 가게");
            assertThat(result.customerNickname()).isEqualTo("테스터");
        }
    }

    @Nested
    @DisplayName("getReviewsByStore — 가게 리뷰 목록 조회")
    class GetReviewsByStore {

        private StoreEntity sampleStore() {
            return StoreEntity.builder()
                    .ownerId(UUID.randomUUID())
                    .categoryId(UUID.randomUUID())
                    .areaId(UUID.randomUUID())
                    .name("테스트 가게")
                    .address("서울시 강남구")
                    .phone("010-0000-0000")
                    .build();
        }

        @Test
        @DisplayName("평점 필터 없음 → 전체 리뷰 반환")
        void noRatingFilter_returnsAllReviews() {
            // given
            StoreEntity store = sampleStore();
            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));

            ReviewEntity review = mock(ReviewEntity.class);
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
            given(reviewRepository.findByStoreId(eq(STORE_ID), any()))
                    .willReturn(new PageImpl<>(List.of(review), pageable, 1L));

            // when
            Page<ResReviewDto> result = reviewService.getReviewsByStore(STORE_ID, null, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(reviewRepository).findByStoreId(eq(STORE_ID), any());
            verify(reviewRepository, never()).findByStoreIdAndRating(any(), anyInt(), any());
        }

        @Test
        @DisplayName("평점 필터 있음 → 해당 평점 리뷰만 반환")
        void ratingFilter_returnsFilteredReviews() {
            // given
            StoreEntity store = sampleStore();
            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));

            ReviewEntity review = mock(ReviewEntity.class);
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
            given(reviewRepository.findByStoreIdAndRating(eq(STORE_ID), eq(5), any()))
                    .willReturn(new PageImpl<>(List.of(review), pageable, 1L));

            // when
            Page<ResReviewDto> result = reviewService.getReviewsByStore(STORE_ID, 5, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(reviewRepository).findByStoreIdAndRating(eq(STORE_ID), eq(5), any());
            verify(reviewRepository, never()).findByStoreId(any(), any());
        }

        @Test
        @DisplayName("허용되지 않는 페이지 사이즈(20) → 기본값 10으로 보정")
        void invalidPageSize_correctedToDefault() {
            // given
            StoreEntity store = sampleStore();
            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));
            given(reviewRepository.findByStoreId(eq(STORE_ID), any())).willReturn(Page.empty());

            Pageable invalidPageable = PageRequest.of(0, 20);

            // when
            reviewService.getReviewsByStore(STORE_ID, null, invalidPageable);

            // then
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(reviewRepository).findByStoreId(eq(STORE_ID), captor.capture());
            assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        }
    }
}