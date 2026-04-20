package com.example.delivery.store.domain.entity;

import com.example.delivery.area.domain.entity.AreaEntity;
import com.example.delivery.category.domain.entity.CategoryEntity;
import com.example.delivery.global.infrastructure.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Table(name = "p_store")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "store_id",  nullable = false, updatable = false)
    private UUID id;

    /**
     * User 도메인과는 ID만 참조하도록 함
     */
    @Column(length = 10, nullable = false)
    private Long ownerId;

    /**
     * Category 및 Area 도메인은 Store에서만 사용하므로 엔티티를 참조함
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private AreaEntity area;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 255, nullable = false)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(precision = 2, scale = 1, nullable = false)
    private BigDecimal averageRating;

    @Column(nullable = false)
    private boolean isHidden;

    @Builder
    public StoreEntity(Long ownerId, CategoryEntity category, AreaEntity area, String name, String address, String phone) {
        this.ownerId = ownerId;
        this.category = category;
        this.area = area;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.averageRating = BigDecimal.ZERO;
        this.isHidden = false;
    }

    /**
     * 연관관계 메서드
     */
    public void updateAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    public void toggleHidden() {
        this.isHidden = !this.isHidden;
    }
}
