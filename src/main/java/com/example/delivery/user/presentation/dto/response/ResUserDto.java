package com.example.delivery.user.presentation.dto.response;

import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "사용자 응답")
public record ResUserDto(
        @Schema(description = "아이디", example = "user01") String username,
        @Schema(description = "닉네임", example = "홍길동") String nickname,
        @Schema(description = "이메일", example = "user01@example.com") String email,
        @Schema(description = "권한", example = "CUSTOMER") UserRole role,
        @Schema(description = "공개 여부", example = "true") boolean isPublic,
        @Schema(description = "가입 시각") LocalDateTime createdAt
) {

    public static ResUserDto from(UserEntity user) {
        return new ResUserDto(
                user.getUsername().value(),
                user.getNickname(),
                user.getEmail().value(),
                user.getRole(),
                user.isPublic(),
                user.getCreatedAt());
    }
}
