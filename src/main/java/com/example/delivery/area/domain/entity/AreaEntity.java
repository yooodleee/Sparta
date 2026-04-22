package com.example.delivery.area.domain.entity;

import com.example.delivery.global.infrastructure.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@Table(name = "p_area")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AreaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "area_id",  nullable = false, updatable = false)
    private UUID id;

    @Column(length = 100, nullable = false, unique = true)
    private String name;

    @Column(length = 50, nullable = false)
    private String city;

    @Column(length = 50, nullable = false)
    private String district;

    @Column(nullable = false)
    private boolean isActive;

    /**
     * name: 지역명 (예: 광화문)
     * city: 시/도
     * district: 구/군
     * isActive: 운영 활성화 여부 (default = true)
     */
    @Builder
    public AreaEntity(String name, String city, String district, boolean isActive) {
        this.name = name;
        this.city = city;
        this.district = district;
        this.isActive = isActive;
    }

    /**
     * 활성화 여부 변경 메서드
     */
    public void toggleActive() {
        this.isActive = !this.isActive;
    }
}
