package com.example.delivery.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 정보 수정 요청 (변경할 필드만 전달)")
public record ReqUpdateUser(
        @Schema(description = "닉네임", example = "길동이")
        @Size(max = 100) String nickname,
        @Schema(description = "이메일", example = "new@example.com")
        @Size(max = 255) String email,
        @Schema(description = "새 비밀번호 (본인만 변경 가능)", example = "NewPass1!")
        @Size(min = 8, max = 15) String password,
        @Schema(description = "현재 비밀번호 (password 변경 시 필수)", example = "OldPass1!")
        @Size(min = 8, max = 15) String currentPassword,
        @Schema(description = "공개 여부 플래그", example = "true")
        Boolean isPublic
) {

}
