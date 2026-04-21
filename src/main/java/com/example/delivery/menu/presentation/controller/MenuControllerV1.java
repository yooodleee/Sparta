package com.example.delivery.menu.presentation.controller;

import com.example.delivery.global.common.response.ApiResponse;
import com.example.delivery.menu.application.service.MenuServiceV1;
import com.example.delivery.menu.presentation.dto.request.ReqCreateMenuDto;
import com.example.delivery.menu.presentation.dto.response.ResMenuDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Menu API", description = "메뉴 도메인 관련 API (생성 및 조회)")
public class MenuControllerV1 {

    private final MenuServiceV1 menuService;

    @PostMapping("/stores/{storeId}/menus")
    @Operation(summary = "메뉴 등록", description = "가게에 새로운 메뉴를 등록합니다. (201 Created 반환)")
    public ResponseEntity<ApiResponse<ResMenuDto>> createMenu(
            @PathVariable UUID storeId,
            @RequestBody ReqCreateMenuDto request){

        ResMenuDto response = menuService.createMenu(storeId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/stores/{storeId}/menus")
    @Operation(summary = "가게 메뉴 목록 조회", description = "특정 가게의 메뉴를 조회합니다. keyword 파라미터로 상품명 검색이 가능합니다.")
    public ResponseEntity<ApiResponse<List<ResMenuDto>>> getVisibleMenus(
            @PathVariable UUID storeId,
            @RequestParam(required = false) String keyword){

        List<ResMenuDto> response = menuService.getVisibleMenus(storeId, keyword);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/menus/{menuId}")
    @Operation(summary = "메뉴 단건 상세 조회", description = "메뉴 ID로 단일 메뉴의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<ResMenuDto>> getMenu(
            @PathVariable UUID menuId){

        ResMenuDto response = menuService.getMenu(menuId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
