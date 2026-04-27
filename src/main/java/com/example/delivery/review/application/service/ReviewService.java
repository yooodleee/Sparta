package com.example.delivery.review.application.service;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.global.infrastructure.security.UserPrincipal;
import com.example.delivery.order.domain.entity.OrderEntity;
import com.example.delivery.order.domain.entity.OrderStatus;
import com.example.delivery.order.domain.repository.OrderRepository;
import com.example.delivery.review.domain.entity.ReviewEntity;
import com.example.delivery.review.domain.repository.ReviewRepository;
import com.example.delivery.store.domain.entity.StoreEntity;
import com.example.delivery.store.domain.repository.StoreRepository;
import com.example.delivery.review.presentation.dto.request.ReqCreateReviewDto;
import com.example.delivery.review.presentation.dto.request.ReqUpdateReviewDto;
import com.example.delivery.review.presentation.dto.response.ResReviewDto;
import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.user.domain.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    @Transactional
    public ResReviewDto createReview(UUID orderId, ReqCreateReviewDto request, UserPrincipal principal) {

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_NOT_COMPLETED);
        }

        if (!order.getCustomerId().equals(principal.username())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 1주문 1리뷰 중복 검증
        if (reviewRepository.existsByOrderId(orderId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REVIEW);
        }

        // 작성 시점의 닉네임을 스냅샷으로 저장 -> 리뷰 조회 시, User Table 추가 검색 X
        UserEntity customer = userRepository.findByUsername(principal.username())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UUID storeId = order.getStoreId();

        ReviewEntity review = ReviewEntity.builder()
                .orderId(orderId)
                .storeId(storeId)
                .customerId(principal.username())
                .customerNickname(customer.getNickname())
                .rating(request.rating())
                .content(request.content())
                .build();

        ReviewEntity saved = reviewRepository.save(review);
        StoreEntity store = recalculateStoreRating(storeId);

        return ResReviewDto.from(saved, store.getName());
    }

    public ResReviewDto getReview(UUID reviewId) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        StoreEntity store = storeRepository.findById(review.getStoreId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        return ResReviewDto.from(review, store.getName());
    }

    private static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 30, 50);

    public Page<ResReviewDto> getReviewsByStore(UUID storeId, Integer rating, Pageable pageable) {
        int size = ALLOWED_PAGE_SIZES.contains(pageable.getPageSize()) ? pageable.getPageSize() : 10;
        Pageable validatedPageable = PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());

        Page<ReviewEntity> reviews = (rating != null)
                ? reviewRepository.findByStoreIdAndRating(storeId, rating, validatedPageable)
                : reviewRepository.findByStoreId(storeId, validatedPageable);

        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        return reviews.map(r -> ResReviewDto.from(r, store.getName()));
    }

    @Transactional
    public ResReviewDto updateReview(UUID reviewId, ReqUpdateReviewDto request, UserPrincipal principal) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getCustomerId().equals(principal.username())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        review.update(request.rating(), request.content());

        StoreEntity store = recalculateStoreRating(review.getStoreId());

        return ResReviewDto.from(review, store.getName());
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

        recalculateStoreRating(review.getStoreId());
    }

    private StoreEntity recalculateStoreRating(UUID storeId) {
        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
        List<Integer> ratings = reviewRepository.findRatingsByStoreId(storeId);
        store.recalculateAverageRating(ratings);
        return store;
    }
}