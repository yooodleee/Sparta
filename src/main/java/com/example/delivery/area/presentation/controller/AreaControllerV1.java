package com.example.delivery.area.presentation.controller;

import com.example.delivery.area.application.service.AreaServiceV1;
import com.example.delivery.area.presentation.dto.request.ReqCreateAreaDto;
import com.example.delivery.area.presentation.dto.response.ResCreateAreaDto;
import com.example.delivery.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
