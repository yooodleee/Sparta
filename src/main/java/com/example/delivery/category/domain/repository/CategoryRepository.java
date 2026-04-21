package com.example.delivery.category.domain.repository;

import com.example.delivery.category.domain.entity.CategoryEntity;

import java.util.Optional;

public interface CategoryRepository {

    CategoryEntity save(CategoryEntity category);

    Optional<CategoryEntity> findByName(String name);
}
