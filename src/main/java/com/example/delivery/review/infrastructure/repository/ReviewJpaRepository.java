package com.example.delivery.review.infrastructure.repository;

import com.example.delivery.review.domain.entity.ReviewEntity;
import com.example.delivery.review.domain.repository.ReviewRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, UUID>, ReviewRepository {
    // '1주문 1리뷰' 규칙 검증
    boolean existsByOrderId(UUID orderId);

    Page<ReviewEntity> findByStoreId(UUID storeId, Pageable pageable);

    Page<ReviewEntity> findByStoreIdAndRating(UUID storeId, int rating, Pageable pageable);

    // 리뷰 평균 평점 갱신
    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.storeId = :storeId")
    Optional<Double> calculateAverageRating(@Param("storeId") UUID storeId);
}