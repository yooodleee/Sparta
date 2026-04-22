package com.example.delivery.menu.presentation.controller;

import com.example.delivery.global.common.response.ApiResponse;
import com.example.delivery.menu.application.service.MenuServiceV1;
import com.example.delivery.menu.presentation.dto.request.ReqCreateMenuDto;
import com.example.delivery.menu.presentation.dto.request.ReqUpdateMenuDto;
import com.example.delivery.menu.presentation.dto.response.ResMenuDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Menu API", description = "메뉴 도메인 관련 API")
public class MenuControllerV1 {

    private final MenuServiceV1 menuService;

    @PostMapping("/stores/{storeId}/menus")
    @Operation(summary = "메뉴 등록", description = "가게에 새로운 메뉴를 등록합니다. (201 Created 반환)")
    public ResponseEntity<ApiResponse<ResMenuDto>> createMenu(
            @PathVariable UUID storeId,
            @RequestBody ReqCreateMenuDto request){

        ResMenuDto response = menuService.createMenu(storeId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @GetMapping("/stores/{storeId}/menus")
    @Operation(summary = "가게 메뉴 목록 조회", description = "특정 가게의 메뉴를 조회합니다. keyword 파라미터로 상품명 검색이 가능합니다.")
    public ResponseEntity<ApiResponse<List<ResMenuDto>>> getVisibleMenus(
            @PathVariable UUID storeId,
            @RequestParam(required = false) String keyword){

        List<ResMenuDto> response = menuService.getVisibleMenus(storeId, keyword);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/menus/{menuId}")
    @Operation(summary = "메뉴 단건 상세 조회", description = "메뉴 ID로 단일 메뉴의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<ResMenuDto>> getMenu(
            @PathVariable UUID menuId){

        ResMenuDto response = menuService.getMenu(menuId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/menus/{menuId}")
    @Operation(summary = "메뉴 수정", description = "메뉴의 이름, 설명, 가격, 이미지를 수정합니다.")
    public ResponseEntity<ApiResponse<ResMenuDto>> updateMenu(
            @PathVariable UUID menuId,
            @RequestBody ReqUpdateMenuDto request){

        ResMenuDto response = menuService.updateMenu(menuId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    //메뉴 품절 상태 토글
    @PatchMapping("/menus/{menuId}/sold-out")
    @Operation(summary = "메뉴 품절 토글", description = "메뉴의 품절 상태를 변경합니다.")
    public ResponseEntity<ApiResponse<ResMenuDto>> toggleSoldOut(
            @PathVariable UUID menuId){

        ResMenuDto response = menuService.toggleSoldOut(menuId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/menus/{menuId}/visibility")
    @Operation(summary = "메뉴 숨김 상태 변경", description = "메뉴를 숨기거나 다시 노출시킵니다.(T/F)")
    public ResponseEntity<ApiResponse<ResMenuDto>> updateVisibility(
            @PathVariable UUID menuId,
            @RequestParam boolean isHidden){

        ResMenuDto response = menuService.updateVisibility(menuId, isHidden);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/menus/{menuId}")
    @Operation(summary = "메뉴 삭제", description = "메뉴를 삭제합니다. (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteMenu(
            @PathVariable UUID menuId){

        menuService.deleteMenu(menuId);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
