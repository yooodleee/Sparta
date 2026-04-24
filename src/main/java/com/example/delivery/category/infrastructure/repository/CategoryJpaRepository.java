package com.example.delivery.category.infrastructure.repository;

import com.example.delivery.category.domain.entity.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, UUID> {

    Optional<CategoryEntity> findByName(String name);

    Optional<CategoryEntity> findByNameAndDeletedAtIsNotNull(String name);

    Optional<CategoryEntity> findByIdAndDeletedAtIsNull(UUID id);

    Page<CategoryEntity> findAllByDeletedAtIsNull(Pageable pageable);

    Page<CategoryEntity> findByNameContainingAndDeletedAtIsNull(String keyword, Pageable pageable);
}
