package com.example.delivery.store.presentation.controller;

import com.example.delivery.global.common.response.ApiResponse;
import com.example.delivery.global.common.response.PageResponse;
import com.example.delivery.global.infrastructure.security.UserPrincipal;
import com.example.delivery.store.application.service.StoreServiceV1;
import com.example.delivery.store.presentation.dto.request.ReqCreateStoreDto;
import com.example.delivery.store.presentation.dto.request.ReqUpdateStoreDto;
import com.example.delivery.store.presentation.dto.response.ResCreateStoreDto;
import com.example.delivery.store.presentation.dto.response.ResGetStoreDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreControllerV1 {

    private final StoreServiceV1 storeService;

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<ResCreateStoreDto> createStore(
            @Valid @RequestBody ReqCreateStoreDto request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.created(storeService.createStore(request, userPrincipal.id()));
    }

    @GetMapping
    public ApiResponse<PageResponse<ResGetStoreDto>> getAllStores(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID areaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(storeService.getAllStores(keyword, categoryId, areaId, page, size));
    }

    @GetMapping("/{storeId}")
    public ApiResponse<ResGetStoreDto> getStore(@PathVariable UUID storeId) {
        return ApiResponse.ok(storeService.getStore(storeId));
    }

    @PatchMapping("/{storeId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    public ApiResponse<ResGetStoreDto> updateStore(
            @PathVariable UUID storeId,
            @Valid @RequestBody ReqUpdateStoreDto request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.ok(storeService.updateStore(storeId, request, userPrincipal));
    }

    @PatchMapping("/{storeId}/hide")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    public ApiResponse<ResGetStoreDto> toggleHiddenStore(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.ok(storeService.toggleHiddenStore(storeId, userPrincipal));
    }

    @DeleteMapping("/{storeId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MASTER')")
    public ApiResponse<Void> deleteStore(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        storeService.deleteStore(storeId, userPrincipal);
        return ApiResponse.ok(null);
    }
}
