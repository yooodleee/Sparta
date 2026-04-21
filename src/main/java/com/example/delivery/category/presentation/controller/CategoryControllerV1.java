package com.example.delivery.category.presentation.controller;

import com.example.delivery.category.application.service.CategoryServiceV1;
import com.example.delivery.category.presentation.dto.request.ReqCreateCategoryDto;
import com.example.delivery.category.presentation.dto.request.ReqUpdateCategoryDto;
import com.example.delivery.category.presentation.dto.response.ResCreateCategoryDto;
import com.example.delivery.category.presentation.dto.response.ResGetCategoryDto;
import com.example.delivery.global.common.response.ApiResponse;
import com.example.delivery.global.common.response.PageResponse;
import com.example.delivery.global.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryControllerV1 {

    private final CategoryServiceV1 categoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    public ApiResponse<ResCreateCategoryDto> createCategory(@Valid @RequestBody ReqCreateCategoryDto request) {
        return ApiResponse.created(categoryService.createCategory(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<ResGetCategoryDto>> getAllCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(categoryService.getAllCategories(keyword, page, size));
    }

    @GetMapping("/{categoryId}")
    public ApiResponse<ResGetCategoryDto> getCategory(@PathVariable UUID categoryId) {
        return ApiResponse.ok(categoryService.getCategory(categoryId));
    }

    @PatchMapping("/{categoryId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    public ApiResponse<ResGetCategoryDto> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody ReqUpdateCategoryDto request
    ) {
        return ApiResponse.ok(categoryService.updateCategory(categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasAnyRole('MASTER')")
    public ApiResponse<Void> deleteCategory(
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        categoryService.deleteCategory(categoryId, userPrincipal.username());
        return ApiResponse.ok(null);
    }
}
