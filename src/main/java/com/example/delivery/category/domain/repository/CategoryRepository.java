package com.example.delivery.category.domain.repository;

import com.example.delivery.category.domain.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
}
