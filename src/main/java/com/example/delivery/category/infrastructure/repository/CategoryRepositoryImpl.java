package com.example.delivery.category.infrastructure.repository;

import com.example.delivery.category.domain.entity.CategoryEntity;
import com.example.delivery.category.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository categoryJpaRepository;

    @Override
    public CategoryEntity save(CategoryEntity category) {
        return categoryJpaRepository.save(category);
    }

    @Override
    public Optional<CategoryEntity> findByName(String name) {
        return categoryJpaRepository.findByName(name);
    }
}
