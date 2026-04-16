package com.example.delivery.user.presentation.controller;

import com.example.delivery.global.common.response.ApiResponse;
import com.example.delivery.user.application.service.UserServiceV1;
import com.example.delivery.user.presentation.dto.response.ResUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserControllerV1 {

    private final UserServiceV1 userService;

    @GetMapping("/{username}")
    public ApiResponse<ResUserDto> getUser(@PathVariable String username) {
        return ApiResponse.ok(userService.findByUsername(username));
    }
}
