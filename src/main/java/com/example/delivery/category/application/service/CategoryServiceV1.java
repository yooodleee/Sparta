package com.example.delivery.category.application.service;

import com.example.delivery.category.application.exception.CategoryAlreadyExistsException;
import com.example.delivery.category.domain.entity.CategoryEntity;
import com.example.delivery.category.domain.repository.CategoryRepository;
import com.example.delivery.category.presentation.dto.request.ReqCreateCategoryDto;
import com.example.delivery.category.presentation.dto.response.ResCreateCategoryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceV1 {

    private final CategoryRepository categoryRepository;

    @Transactional
    public ResCreateCategoryDto createCategory(@Valid ReqCreateCategoryDto request) {

        String categoryName = request.name().trim();

        // 이미 존재하는 카테고리명인지 확인
        validateDuplicateCategoryName(categoryName);

        CategoryEntity category = CategoryEntity.builder()
                .name(categoryName)
                .build();

        return ResCreateCategoryDto.from(categoryRepository.save(category));
    }

    private void validateDuplicateCategoryName(String categoryName) {
        if (categoryRepository.findByName(categoryName).isPresent()) {
            throw new CategoryAlreadyExistsException();
        }
    }
}
