package com.example.delivery.user.presentation.controller;

import com.example.delivery.global.common.response.ApiResponse;
import com.example.delivery.user.application.service.AuthService;
import com.example.delivery.user.presentation.dto.request.ReqLogin;
import com.example.delivery.user.presentation.dto.request.ReqSignup;
import com.example.delivery.user.presentation.dto.response.ResLogin;
import com.example.delivery.user.presentation.dto.response.ResSignup;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResSignup> signup(@Valid @RequestBody ReqSignup req) {
        return ApiResponse.created(authService.signup(req));
    }

    @PostMapping("/login")
    public ApiResponse<ResLogin> login(@Valid @RequestBody ReqLogin req) {
        return ApiResponse.ok(authService.login(req));
    }
}
