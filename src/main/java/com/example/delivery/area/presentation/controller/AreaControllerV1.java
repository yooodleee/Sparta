package com.example.delivery.area.presentation.controller;

import com.example.delivery.area.application.service.AreaServiceV1;
import com.example.delivery.area.presentation.dto.request.ReqUpdateAreaDto;
import com.example.delivery.area.presentation.dto.request.ReqCreateAreaDto;
import com.example.delivery.area.presentation.dto.response.ResCreateAreaDto;
import com.example.delivery.area.presentation.dto.response.ResGetAreaDto;
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
@RequestMapping("/api/v1/areas")
@RequiredArgsConstructor
public class AreaControllerV1 {

    private final AreaServiceV1 areaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    public ApiResponse<ResCreateAreaDto> createArea(@Valid @RequestBody ReqCreateAreaDto request) {
        return ApiResponse.created(areaService.createArea(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<ResGetAreaDto>> getAllAreas(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(areaService.getAllAreas(keyword, page, size));
    }

    @GetMapping("/{areaId}")
    public ApiResponse<ResGetAreaDto> getArea(@PathVariable UUID areaId) {
        return ApiResponse.ok(areaService.getArea(areaId));
    }

    @PatchMapping("/{areaId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
    public ApiResponse<ResGetAreaDto> updateArea(
            @PathVariable UUID areaId,
            @Valid @RequestBody ReqUpdateAreaDto request
    ) {
        return ApiResponse.ok(areaService.updateArea(areaId, request));
    }

    @DeleteMapping("/{areaId}")
    @PreAuthorize("hasAnyRole('MASTER')")
    public ApiResponse<Void> deleteArea(
            @PathVariable UUID areaId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        areaService.deleteArea(areaId, userPrincipal.username());
        return ApiResponse.ok(null);
    }
}
