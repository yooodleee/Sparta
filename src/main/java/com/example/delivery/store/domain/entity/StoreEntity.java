package com.example.delivery.store.domain.entity;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.global.infrastructure.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Table(
        name = "p_store",
        indexes = {
                @Index(name = "idx_store_owner", columnList = "owner_id"),
                @Index(name = "idx_store_category", columnList = "category_id"),
                @Index(name = "idx_store_area", columnList = "area_id"),
                @Index(name = "idx_store_name", columnList = "name")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreEntity extends BaseEntity {

    private static final BigDecimal DEFAULT_RATING =
            BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
    private static final BigDecimal MAX_RATING = new BigDecimal("5.0");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "store_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "area_id", nullable = false)
    private UUID areaId;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 255, nullable = false)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(name = "average_rating", precision = 2, scale = 1, nullable = false)
    private BigDecimal averageRating;

    @Column(name = "is_hidden", nullable = false)
    private boolean isHidden;

    @Builder
    private StoreEntity(UUID ownerId, UUID categoryId, UUID areaId, String name, String address, String phone) {
        this.ownerId = ownerId;
        this.categoryId = categoryId;
        this.areaId = areaId;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.averageRating = DEFAULT_RATING;
        this.isHidden = false;
    }

    /**
     * 가게 기본 정보 수정. phone 은 null 허용(미입력 시 null 로 덮어씀)
     */
    public void update(UUID categoryId, UUID areaId, String name, String address, String phone) {
        this.categoryId = categoryId;
        this.areaId = areaId;
        this.name = name;
        this.address = address;
        this.phone = phone;
    }

    /**
     * 숨김 토글 (삭제와 독립적으로 동작)
     */
    public void toggleHidden() {
        this.isHidden = !this.isHidden;
    }

    /**
     * 리뷰 CUD 시 평균 평점을 재계산
     * 도메인 불변식: 0.0 ≤ averageRating ≤ 5.0
     */
    public void recalculateAverageRating(List<Integer> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            this.averageRating = DEFAULT_RATING;
            return;
        }

        double average = ratings.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        BigDecimal rounded = BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP);
        if (rounded.compareTo(BigDecimal.ZERO) < 0 || rounded.compareTo(MAX_RATING) > 0) {
            throw new BusinessException(ErrorCode.INVALID_STORE_RATING);
        }

        this.averageRating = rounded;
    }

    /**
     * 본인 가게 여부
     */
    public boolean isOwnedBy(UUID ownerId) {
        return this.ownerId.equals(ownerId);
    }
}
