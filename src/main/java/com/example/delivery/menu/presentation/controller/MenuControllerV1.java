package com.example.delivery.menu.presentation.controller;

import com.example.delivery.global.common.response.ApiResponse;
import com.example.delivery.global.common.response.PageResponse;
import com.example.delivery.global.infrastructure.security.UserPrincipal;
import com.example.delivery.menu.application.service.MenuServiceV1;
import com.example.delivery.menu.domain.repository.MenuSearchCondition;
import com.example.delivery.menu.presentation.dto.request.ReqCreateMenuDto;
import com.example.delivery.menu.presentation.dto.request.ReqUpdateMenuDto;
import com.example.delivery.menu.presentation.dto.response.ResMenuDto;
import com.example.delivery.user.domain.entity.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
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
    @Operation(summary = "가게 메뉴 목록 조회",
            description = "특정 가게에 등록된 메뉴 목록을 조회합니다. (키워드, 가격대, 기간 필터 지원)<br><br>" +
                          "[권한별 데이터 노출 기준]<br>" +
                        "CUSTOMER(일반 고객)<br>" + "-가게(Store)와 메뉴(Menu)가 모두 정상 상태(`isHidden=false`,`deletedAt=null`인 데이터만 반환됩니다.<br><br>" +
                        "OWNER / MANAGER / MASTER<br>" + "-숨김 처리되거나 삭제된 메뉴, 혹은 숨김 처리된 가게의 메뉴도 모두 포함하여 조회할 수 있습니다. (권한 검증 통과 시)" )

    public ResponseEntity<ApiResponse<PageResponse<ResMenuDto>>> getMenus(
    @PathVariable UUID storeId,
            @ParameterObject @ModelAttribute MenuSearchCondition condition,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        //log.info("포스트맨에서 넘어온 검색 조건 확인 : {}",condition);
        //UserRole userRole = UserRole.CUSTOMER;
        //UserRole userRole = UserRole.OWNER;
        //log.info("포스트맨 파라미터: {}, 현재 내 권한: {}", condition, userRole);


        UserRole userRole = (userPrincipal != null) ? userPrincipal.role() : UserRole.CUSTOMER;

        Page<ResMenuDto> response = menuService.getMenusWithCondition(storeId, condition, pageable, userRole);

        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(response)));
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
            @PathVariable UUID menuId,
            @AuthenticationPrincipal UserPrincipal userPrincipal){

        menuService.deleteMenu(menuId, userPrincipal.getName());

        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
