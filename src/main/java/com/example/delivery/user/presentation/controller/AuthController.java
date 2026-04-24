package com.example.delivery.user.presentation.controller;

import com.example.delivery.global.common.response.ApiResponse;
import com.example.delivery.user.application.service.AuthService;
import com.example.delivery.user.presentation.dto.request.ReqLogin;
import com.example.delivery.user.presentation.dto.request.ReqSignup;
import com.example.delivery.user.presentation.dto.response.ResLogin;
import com.example.delivery.user.presentation.dto.response.ResSignup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API 컨트롤러.
 *
 * - 회원가입(signup), 로그인(login) 두 가지 진입점을 제공한다.
 * - 두 엔드포인트 모두 비인증 경로이며, SecurityConfig 의 PUBLIC_PATHS 에 등록되어 있다.
 * - 클래스 레벨 {@code @SecurityRequirements}(empty)로 Swagger UI 의 전역 bearerAuth 요구를 해제한다.
 * - 발급된 JWT(HS256)는 이후 모든 보호 엔드포인트에서 {@code Authorization: Bearer ...} 헤더로 전달한다.
 */
@Tag(name = "Auth", description = "회원가입/로그인")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입 — POST /api/v1/auth/signup
     *
     * 1. ReqSignup 의 물리 검증(NotBlank/Size)을 통과하면 AuthService.signup 으로 위임.
     * 2. username/password/email 도메인 검증 → username·email 중복 확인 → BCrypt 해시 저장.
     * 3. MANAGER/MASTER 는 이 API 로 가입할 수 없고, 추후 MASTER 가 PATCH /{username}/role 로 승격시킨다.
     */
    @Operation(
            summary = "회원가입",
            description = """
                    CUSTOMER 또는 OWNER 권한으로 신규 유저를 등록한다.

                    **검증 순서**
                    1. `ReqSignup` 의 물리 검증 (NotBlank, Size).
                    2. `Username`/`Email`/`Password` 도메인 규칙(정규식·길이·복잡도).
                    3. username/email 중복 확인 → 409.
                    4. BCrypt 해시 저장 후 `{ username, nickname, email, role, createdAt }` 반환.

                    **제약**
                    - `role` 은 CUSTOMER 또는 OWNER 만 허용. MANAGER/MASTER 로 가입 시도 시 `SIGNUP_ROLE_NOT_ALLOWED`.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "VALIDATION_ERROR / SIGNUP_ROLE_NOT_ALLOWED / INVALID_USERNAME / INVALID_EMAIL / INVALID_PASSWORD"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "DUPLICATE_USERNAME / DUPLICATE_EMAIL")
    })
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResSignup> signup(@Valid @RequestBody ReqSignup req) {
        return ApiResponse.created(authService.signup(req));
    }

    /**
     * 로그인 — POST /api/v1/auth/login
     *
     * 1. findByUsername 으로 사용자 조회 (실패 시 401).
     * 2. BCryptPasswordEncoder.matches 로 비밀번호 일치 검증 (실패 시 401).
     * 3. JwtTokenProvider.issue 로 HS256 JWT 발급.
     * 4. 응답: { accessToken, username, role }.
     */
    @Operation(
            summary = "로그인",
            description = """
                    username/password 를 검증하고 JWT(HS256) 를 발급한다.

                    **흐름**
                    1. `findByUsername` → 없음이면 401 UNAUTHORIZED.
                    2. `passwordEncoder.matches` → 불일치면 401 UNAUTHORIZED.
                    3. `JwtTokenProvider.issue` 로 accessToken 생성 후 반환.

                    **사용법**
                    - 응답의 `accessToken` 을 복사해서 Swagger UI 상단 "Authorize" 버튼 → `bearerAuth` 입력란에 붙여넣으면 이후 요청이 자동으로 인증 헤더를 단다.
                    - 직접 호출 시에는 `Authorization: Bearer <token>` 헤더로 보낸다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "JWT 발급"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "UNAUTHORIZED — 사용자 없음 또는 비밀번호 불일치")
    })
    @PostMapping("/login")
    public ApiResponse<ResLogin> login(@Valid @RequestBody ReqLogin req) {
        return ApiResponse.ok(authService.login(req));
    }
}
