package com.example.delivery.review.application.service;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.global.infrastructure.security.UserPrincipal;
import com.example.delivery.order.domain.entity.OrderEntity;
import com.example.delivery.order.domain.entity.OrderStatus;
import com.example.delivery.order.infrastructure.repository.OrderRepository;
import com.example.delivery.review.domain.entity.ReviewEntity;
import com.example.delivery.review.domain.repository.ReviewRepository;
import com.example.delivery.review.presentation.dto.request.ReqCreateReviewDto;
import com.example.delivery.review.presentation.dto.request.ReqUpdateReviewDto;
import com.example.delivery.review.presentation.dto.response.ResReviewDto;
import com.example.delivery.user.domain.entity.UserRole;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public ResReviewDto createReview(UUID orderId, ReqCreateReviewDto request, UserPrincipal principal) {

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_NOT_COMPLETED);
        }

        // TODO: [Order 구현 이후] 본인 주문 여부 검증
        // if (!order.getCustomerId().equals(principal.username())) {
        //     throw new BusinessException(ErrorCode.FORBIDDEN);
        // }

        // 1주문 1리뷰 중복 검증
        if (reviewRepository.existsByOrderId(orderId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REVIEW);
        }

        // TODO: [Order 구현 이후] storeId는 Order 엔티티에서 추출
        // UUID storeId = order.getStoreId();
        UUID storeId = request.storeId();

        ReviewEntity review = ReviewEntity.builder()
                .orderId(orderId)
                .storeId(storeId)
                .customerId(principal.username())
                .rating(request.rating())
                .content(request.content())
                .build();

        ReviewEntity saved = reviewRepository.save(review);

        // TODO: [Store 구현 이후] 가게 평균 평점 재집계
        // recalculateAverageRating(storeId);

        // TODO: [Store/User 구현 이후] 실제 storeName, customerNickname 조회
        return ResReviewDto.from(saved, "임시 가게명", "임시 닉네임");
    }

    public Page<ResReviewDto> getReviewsByStore(UUID storeId, Integer rating, Pageable pageable) {
        Page<ReviewEntity> reviews = (rating != null)
                ? reviewRepository.findByStoreIdAndRating(storeId, rating, pageable)
                : reviewRepository.findByStoreId(storeId, pageable);

        // TODO: [Store/User 구현 이후] 실제 storeName, customerNickname 조회
        return reviews.map(r -> ResReviewDto.from(r, "임시 가게명", "임시 닉네임"));
    }

    @Transactional
    public ResReviewDto updateReview(UUID reviewId, ReqUpdateReviewDto request, UserPrincipal principal) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getCustomerId().equals(principal.username())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        review.update(request.rating(), request.content());

        // TODO: [Store 구현 이후] 가게 평균 평점 재집계
        // recalculateAverageRating(review.getStoreId());

        // TODO: [Store/User 구현 이후] 실제 storeName, customerNickname 조회
        return ResReviewDto.from(review, "임시 가게명", "임시 닉네임");
    }

    @Transactional
    public void deleteReview(UUID reviewId, UserPrincipal principal) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        boolean isAdminRole = principal.role() == UserRole.MANAGER || principal.role() == UserRole.MASTER;
        if (!isAdminRole && !review.getCustomerId().equals(principal.username())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        review.delete(principal.username());

        // TODO: [Store 구현 이후] 가게 평균 평점 재집계
        // recalculateAverageRating(review.getStoreId());
    }
}