package com.example.delivery.user.presentation.controller;

import com.example.delivery.global.common.response.ApiResponse;
import com.example.delivery.global.infrastructure.security.UserPrincipal;
import com.example.delivery.user.application.service.TestAuthService;
import com.example.delivery.user.presentation.dto.response.ResCreateTestUserDto;
import com.example.delivery.user.presentation.dto.response.ResTestMeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 테스트 전용 컨트롤러.
 * <p>
 * - {@code GET /api/v1/test/users} — 파라미터 없이 임시 사용자 생성 후 JWT 발급 - {@code GET /api/v1/test/me}    —
 * {@link UserPrincipal} 주입 확인 (토큰 필수)
 * <p>
 * 실제 회원가입/로그인/사용자 조회 엔드포인트가 정식으로 붙으면 이 컨트롤러와 {@code SecurityConfig}의 {@code /api/v1/test/**} 화이트리스트는 함께 제거한다.
 */
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestAuthControllerV1 {

    private final TestAuthService testAuthService;

    @GetMapping("/users")
    public ApiResponse<ResCreateTestUserDto> createQuickTestUser() {
        return ApiResponse.ok(testAuthService.createDefaultAndIssueToken());
    }

    @GetMapping("/me")
    public ApiResponse<ResTestMeDto> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(ResTestMeDto.from(principal));
    }
}
