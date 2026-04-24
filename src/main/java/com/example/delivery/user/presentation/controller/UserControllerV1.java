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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * 사용자 API 컨트롤러.
 *
 * - 본인/MANAGER/MASTER 권한 축으로 CRUD 를 분기한다.
 *   · 본인만 가능: password 변경(currentPassword 동반 필수).
 *   · 본인/MANAGER/MASTER: 조회/수정(password 제외).
 *   · MANAGER/MASTER: 목록, Soft Delete.
 *   · MASTER 전용: 권한 변경(`PATCH /{username}/role`), 대상이 privileged(MANAGER/MASTER)인 CUD.
 * - Privileged 예외 규칙: 대상 role 이 MANAGER/MASTER 인 경우 위 규칙에서 "본인 또는 MASTER" 만 허용.
 * - 인증 파이프라인: JwtAuthenticationFilter 가 매 요청 DB role/soft-delete 재검증 후 SecurityContext 주입.
 * - Swagger 기준: 클래스가 전역 bearerAuth 요구를 상속한다. AuthController 와 달리 해제하지 않는다.
 */
@Tag(name = "User", description = "사용자 조회/수정/삭제/권한 변경")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserControllerV1 {

    private final UserService userService;

    /**
     * 사용자 목록 — GET /api/v1/users
     *
     * keyword(부분 일치) + role 필터를 조합해 페이지네이션으로 반환한다.
     * MANAGER/MASTER 가 아닌 사용자가 호출하면 서비스 레이어에서 FORBIDDEN.
     */
    @Operation(
            summary = "사용자 목록",
            description = """
                    keyword(username/nickname/email 부분 일치) + role 필터를 조합하여 사용자 목록을 조회한다.

                    **권한**
                    - MANAGER/MASTER 만 호출 가능. 그 외는 403 FORBIDDEN.

                    **페이지네이션**
                    - `page` 0-based, `size` 는 10/30/50 (그 외 값은 10 으로 보정 예정), `sort` 기본 `createdAt,DESC`.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록 반환"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증/토큰 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "MANAGER/MASTER 아님")
    })
    @GetMapping
    public ApiResponse<PageResponse<ResUserDto>> list(
            @Parameter(description = "username/nickname/email 부분 일치 검색어", example = "alice")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "권한 필터") @RequestParam(required = false) UserRole role,
            @Parameter(description = "page=0-based, size={10|30|50}, sort=필드,ASC|DESC")
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal me) {
        return ApiResponse.ok(userService.search(keyword, role, pageable, LoginUser.from(me)));
    }

    /**
     * 본인 정보 조회 — GET /api/v1/users/me
     *
     * 인증된 JWT 소유자 본인의 정보를 반환한다.
     * 내부적으로 {@code userService.getOne(self, self)} 로 위임하며,
     * UserAccessPolicy 의 {@code isSelf} 분기에서 즉시 통과한다.
     */
    @Operation(
            summary = "본인 정보 조회",
            description = """
                    현재 인증된 사용자의 정보를 반환한다. JWT 에서 username 을 추출해 내부적으로 상세 조회 서비스를 재사용한다.

                    **사용 예**
                    - 로그인 직후 프론트가 현재 사용자 프로필을 받을 때.
                    - 클라이언트가 username 을 직접 알 필요가 없다.

                    **인증 실패 동작**
                    - 토큰 없음/만료 → 401.
                    - 권한 변경으로 JWT.role ≠ DB.role → 403.
                    - 계정이 Soft Delete 되었으면 → 401 (JwtAuthenticationFilter 에서 차단).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "본인 정보"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증/토큰 만료/Soft Delete된 계정")
    })
    @GetMapping("/me")
    public ApiResponse<ResUserDto> getMe(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal me) {
        LoginUser actor = LoginUser.from(me);
        return ApiResponse.ok(userService.getOne(actor.username(), actor));
    }

    /**
     * 사용자 상세 — GET /api/v1/users/{username}
     *
     * 본인, MANAGER, MASTER 가 조회 가능하다.
     * 대상 role 이 privileged(MANAGER/MASTER)인 경우에는 본인 또는 MASTER 만 허용(MANAGER 끼리 상호 조회 불가).
     */
    @Operation(
            summary = "사용자 상세",
            description = """
                    특정 사용자의 상세 정보를 반환한다.

                    **권한**
                    - 본인, MANAGER, MASTER.
                    - 대상이 MANAGER/MASTER(privileged)인 경우 본인 또는 MASTER 만 조회 가능.

                    **주의**
                    - `is_public` 필드는 현재 조회 범위를 제어하지 않는다 (예약 필드, 데이터 명세 3.1 참조).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상세 반환"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증/토큰 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인/매니저/마스터 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 사용자 없음")
    })
    @GetMapping("/{username}")
    public ApiResponse<ResUserDto> getOne(
            @Parameter(description = "대상 사용자 아이디", example = "user01") @PathVariable String username,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal me) {
        return ApiResponse.ok(userService.getOne(username, LoginUser.from(me)));
    }

    /**
     * 사용자 정보 수정 — PATCH /api/v1/users/{username}
     *
     * 변경할 필드만 요청 바디에 담는 부분 업데이트.
     * 비밀번호 변경 시에는 세션/토큰 탈취 대비로 현재 비밀번호(currentPassword) 동반 전송을 필수로 요구한다.
     */
    @Operation(
            summary = "사용자 정보 수정",
            description = """
                    부분 업데이트 방식. 바디에 포함된 필드만 변경된다.

                    **권한별 수정 가능 필드**
                    - 본인: `nickname`, `email`, `password`, `isPublic`.
                    - MANAGER/MASTER: `password` 제외 동일. (대상이 privileged 면 본인/MASTER 만 가능)

                    **비밀번호 변경 규칙**
                    - `password` 가 요청에 포함되면 `currentPassword` 필드도 필수.
                    - 누락 → 400 `CURRENT_PASSWORD_REQUIRED`, 불일치 → 401 `INVALID_CURRENT_PASSWORD`.
                    - 본인이 아닌 사용자가 `password` 를 시도하면 정책상 먼저 FORBIDDEN 으로 차단.

                    **이메일 변경**
                    - 다른 사용자가 사용 중이면 409 `DUPLICATE_EMAIL`.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "VALIDATION_ERROR / CURRENT_PASSWORD_REQUIRED / INVALID_USERNAME / INVALID_EMAIL / INVALID_PASSWORD"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "미인증 또는 INVALID_CURRENT_PASSWORD"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 사용자 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "DUPLICATE_EMAIL")
    })
    @PatchMapping("/{username}")
    public ApiResponse<ResUserDto> update(
            @Parameter(description = "대상 사용자 아이디", example = "user01") @PathVariable String username,
            @Valid @RequestBody ReqUpdateUser req,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal me) {
        return ApiResponse.ok(userService.update(username, req, LoginUser.from(me)));
    }

    /**
     * 사용자 Soft Delete — DELETE /api/v1/users/{username}
     *
     * 물리 삭제가 아닌 {@code deleted_at = now(), deleted_by = actor} 로 논리 삭제.
     * 이후 JPA 레벨 {@code @SQLRestriction("deleted_at IS NULL")} 으로 조회에서 자동 제외된다.
     * 삭제된 계정으로 JWT 재사용 시 JwtAuthenticationFilter 가 401 UNAUTHORIZED 로 차단.
     */
    @Operation(
            summary = "사용자 Soft Delete",
            description = """
                    사용자를 논리 삭제한다.

                    **권한**
                    - MANAGER/MASTER 만 가능.
                    - 대상이 MANAGER/MASTER(privileged)인 경우 MASTER 만 가능.
                    - 자기 자신은 삭제 불가 → 400 `CANNOT_DELETE_SELF`.

                    **동작**
                    - `deleted_at`, `deleted_by` 업데이트. 이후 해당 username 으로 로그인/조회 불가.
                    - 기존 JWT 는 자동 무효화 (필터에서 soft-deleted 계정은 401 반환).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공(본문 없음)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "CANNOT_DELETE_SELF"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증/토큰 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 사용자 없음")
    })
    @DeleteMapping("/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete(
            @Parameter(description = "대상 사용자 아이디", example = "user01") @PathVariable String username,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal me) {
        userService.softDelete(username, LoginUser.from(me));
    }

    /**
     * 사용자 권한 변경 — PATCH /api/v1/users/{username}/role
     *
     * MASTER 만 사용 가능한 별도 엔드포인트. 본인의 role 은 변경할 수 없다.
     * 권한 변경 후 다음 요청부터 JwtAuthenticationFilter 의 role 재검증에 의해
     * 기존에 발급된 JWT 는 payload.role ≠ DB.role 로 403 ROLE_MISMATCH 처리된다.
     */
    @Operation(
            summary = "사용자 권한 변경",
            description = """
                    사용자 권한을 변경한다.

                    **권한**
                    - MASTER 전용.
                    - 본인 role 변경은 불가 → 403 FORBIDDEN.

                    **효과**
                    - 변경 후에도 대상의 기존 JWT 토큰 자체는 유효하지만, 매 요청 재검증에서 role 불일치로 403 ROLE_MISMATCH 반환.
                    - 즉 실질적으로 기존 토큰은 무효화되고 대상은 재로그인 필요.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "권한 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증/토큰 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "MASTER 아님 또는 본인 role 변경 시도"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 사용자 없음")
    })
    @PatchMapping("/{username}/role")
    public ApiResponse<ResUserDto> changeRole(
            @Parameter(description = "대상 사용자 아이디", example = "user01") @PathVariable String username,
            @Valid @RequestBody ReqChangeRole req,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal me) {
        return ApiResponse.ok(userService.changeRole(username, req, LoginUser.from(me)));
    }
}
