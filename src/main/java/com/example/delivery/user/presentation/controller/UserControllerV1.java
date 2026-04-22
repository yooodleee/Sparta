package com.example.delivery.user.presentation.controller;

import com.example.delivery.global.common.auth.LoginUser;
import com.example.delivery.global.common.response.ApiResponse;
import com.example.delivery.global.common.response.PageResponse;
import com.example.delivery.global.infrastructure.security.UserPrincipal;
import com.example.delivery.user.application.service.UserService;
import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.user.presentation.dto.request.ReqChangeRole;
import com.example.delivery.user.presentation.dto.request.ReqUpdateUser;
import com.example.delivery.user.presentation.dto.response.ResUserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserControllerV1 {

    private final UserService userService;

    @GetMapping
    public ApiResponse<PageResponse<ResUserDto>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal me) {
        return ApiResponse.ok(userService.search(keyword, role, pageable, LoginUser.from(me)));
    }

    @GetMapping("/{username}")
    public ApiResponse<ResUserDto> getOne(@PathVariable String username,
            @AuthenticationPrincipal UserPrincipal me) {
        return ApiResponse.ok(userService.getOne(username, LoginUser.from(me)));
    }

    @PatchMapping("/{username}")
    public ApiResponse<ResUserDto> update(@PathVariable String username,
            @Valid @RequestBody ReqUpdateUser req,
            @AuthenticationPrincipal UserPrincipal me) {
        return ApiResponse.ok(userService.update(username, req, LoginUser.from(me)));
    }

    @DeleteMapping("/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete(@PathVariable String username,
            @AuthenticationPrincipal UserPrincipal me) {
        userService.softDelete(username, LoginUser.from(me));
    }

    @PatchMapping("/{username}/role")
    public ApiResponse<ResUserDto> changeRole(@PathVariable String username,
            @Valid @RequestBody ReqChangeRole req,
            @AuthenticationPrincipal UserPrincipal me) {
        return ApiResponse.ok(userService.changeRole(username, req, LoginUser.from(me)));
    }
}
