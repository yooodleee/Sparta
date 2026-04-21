package com.example.delivery.category.domain.repository;

import com.example.delivery.category.domain.entity.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {

    CategoryEntity save(CategoryEntity category);

    Optional<CategoryEntity> findByName(String name);

    Optional<CategoryEntity> findByNameIncludingDeleted(String categoryName);

    Optional<CategoryEntity> findById(UUID categoryId);

    Page<CategoryEntity> findAll(Pageable pageable);

    Page<CategoryEntity> findByNameContaining(String keyword, Pageable pageable);
}
