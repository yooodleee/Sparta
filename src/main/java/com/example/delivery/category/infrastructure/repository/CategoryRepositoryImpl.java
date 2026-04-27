package com.example.delivery.category.infrastructure.repository;

import com.example.delivery.category.domain.entity.CategoryEntity;
import com.example.delivery.category.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

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
        return categoryJpaRepository.findByNameAndDeletedAtIsNull(name);
    }

    @Override
    public Optional<CategoryEntity> findByNameIncludingDeleted(String categoryName) {
        return categoryJpaRepository.findByNameAndDeletedAtIsNotNull(categoryName);
    }

    @Override
    public Optional<CategoryEntity> findById(UUID categoryId) {
        return categoryJpaRepository.findByIdAndDeletedAtIsNull(categoryId);
    }

    @Override
    public Page<CategoryEntity> findAll(Pageable pageable) {
        return categoryJpaRepository.findAllByDeletedAtIsNull(pageable);
    }

    @Override
    public Page<CategoryEntity> findByNameContaining(String keyword, Pageable pageable) {
        return categoryJpaRepository.findByNameContainingAndDeletedAtIsNull(keyword, pageable);
    }
}
