package com.example.delivery.category.domain.entity;

import com.example.delivery.global.infrastructure.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Table(name = "p_category")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "category_id",  nullable = false, updatable = false)
    private UUID id;

    @Column(length = 50, nullable = false, unique = true)
    private String name;

    @Builder
    public CategoryEntity(String name) {
        this.name = name;
    }

    /**
     * 카테고리 이름 수정 메서드
     */
    public void updateName(String name) {
        this.name = name;
    }
}
