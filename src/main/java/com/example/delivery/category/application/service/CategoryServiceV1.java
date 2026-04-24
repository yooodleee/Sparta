package com.example.delivery.category.application.service;

import com.example.delivery.category.application.exception.CategoryAlreadyDeletedException;
import com.example.delivery.category.application.exception.CategoryAlreadyExistsException;
import com.example.delivery.category.application.exception.CategoryNotFoundException;
import com.example.delivery.category.domain.entity.CategoryEntity;
import com.example.delivery.category.domain.repository.CategoryRepository;
import com.example.delivery.category.presentation.dto.request.ReqCreateCategoryDto;
import com.example.delivery.category.presentation.dto.request.ReqUpdateCategoryDto;
import com.example.delivery.category.presentation.dto.response.ResCreateCategoryDto;
import com.example.delivery.category.presentation.dto.response.ResGetCategoryDto;
import com.example.delivery.global.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.example.delivery.global.common.pageable.PageableUtils.createPageable;
import static com.example.delivery.global.common.pageable.PageableUtils.hasKeyword;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceV1 {

    private final CategoryRepository categoryRepository;

    @Transactional
    public ResCreateCategoryDto createCategory(ReqCreateCategoryDto request) {

        String categoryName = request.name().trim();

        // 삭제된 카테고리인지 확인
        validateDeletedCategory(categoryName);

        // 이미 존재하는 카테고리명인지 확인
        validateDuplicateCategoryName(categoryName);

        CategoryEntity category = CategoryEntity.builder()
                .name(categoryName)
                .build();

        return ResCreateCategoryDto.from(categoryRepository.save(category));
    }

    public PageResponse<ResGetCategoryDto> getAllCategories(String keyword, int page, int size) {

        Pageable pageable = createPageable(page, size);

        Page<ResGetCategoryDto> result = hasKeyword(keyword)
                ? categoryRepository.findByNameContaining(keyword.trim(), pageable)
                    .map(ResGetCategoryDto::from)
                : categoryRepository.findAll(pageable)
                    .map(ResGetCategoryDto::from);

        return PageResponse.from(result);
    }

    public ResGetCategoryDto getCategory(UUID categoryId) {
        return ResGetCategoryDto.from(getCategoryEntity(categoryId));
    }

    @Transactional
    public ResGetCategoryDto updateCategory(UUID categoryId, ReqUpdateCategoryDto request) {

        CategoryEntity category = getCategoryEntity(categoryId);

        String categoryName = request.name().trim();

        // 이름이 변경된 경우에만 중복 검사를 수행하도록 함
        if (!category.getName().equals(categoryName)) {
            validateDuplicateCategoryName(categoryName);
        }

        category.updateName(categoryName);

        return ResGetCategoryDto.from(category);
    }

    @Transactional
    public void deleteCategory(UUID categoryId, String deletedBy) {
        CategoryEntity category = getCategoryEntity(categoryId);
        category.softDelete(deletedBy);
    }

    private CategoryEntity getCategoryEntity(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(CategoryNotFoundException::new);
    }

    private void validateDeletedCategory(String categoryName) {
        if (categoryRepository.findByNameIncludingDeleted(categoryName).isPresent()) {
            throw new CategoryAlreadyDeletedException();
        }

    }

    private void validateDuplicateCategoryName(String categoryName) {
        if (categoryRepository.findByName(categoryName).isPresent()) {
            throw new CategoryAlreadyExistsException();
        }
    }
}
