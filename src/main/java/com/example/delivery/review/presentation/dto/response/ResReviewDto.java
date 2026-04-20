package com.example.delivery.review.presentation.dto.response;

import com.example.delivery.review.domain.entity.ReviewEntity;
import java.time.LocalDateTime;
import java.util.UUID;

public record ResReviewDto(
        UUID reviewId,
        UUID orderId,
        UUID storeId,
        String storeName,
        String customerId,
        String customerNickname,
        int rating,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ResReviewDto from(ReviewEntity review, String storeName, String customerNickname) {
        return new ResReviewDto(
                review.getReviewId(),
                review.getOrderId(),
                review.getStoreId(),
                storeName,
                review.getCustomerId(),
                customerNickname,
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}