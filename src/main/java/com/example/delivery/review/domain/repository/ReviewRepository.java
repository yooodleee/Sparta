package com.example.delivery.review.domain.repository;

import com.example.delivery.review.domain.entity.ReviewEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewRepository {

    ReviewEntity save(ReviewEntity review);

    Optional<ReviewEntity> findById(UUID reviewId);

    boolean existsByOrderId(UUID orderId);

    Page<ReviewEntity> findByStoreId(UUID storeId, Pageable pageable);

    Page<ReviewEntity> findByStoreIdAndRating(UUID storeId, int rating, Pageable pageable);

    Optional<Double> calculateAverageRating(UUID storeId);
}