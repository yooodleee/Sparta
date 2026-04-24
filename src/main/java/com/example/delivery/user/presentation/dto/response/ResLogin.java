package com.example.delivery.user.presentation.dto.response;

import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record ResLogin(
        @Schema(description = "JWT 액세스 토큰") String accessToken,
        @Schema(description = "아이디", example = "user01") String username,
        @Schema(description = "권한", example = "CUSTOMER") UserRole role
) {

    public static ResLogin of(String accessToken, UserEntity user) {
        return new ResLogin(accessToken, user.getUsername().value(), user.getRole());
    }
}
