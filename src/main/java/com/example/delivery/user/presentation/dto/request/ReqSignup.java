package com.example.delivery.user.presentation.dto.request;

import com.example.delivery.user.domain.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record ReqSignup(
        @Schema(description = "아이디 (소문자/숫자 4~10자)", example = "user01")
        @NotBlank @Size(min = 4, max = 10) String username,
        @Schema(description = "닉네임", example = "홍길동")
        @NotBlank @Size(max = 100) String nickname,
        @Schema(description = "이메일", example = "user01@example.com")
        @NotBlank @Size(max = 255) String email,
        @Schema(description = "비밀번호 (8~15자, 대/소/숫/특수 각 1자 이상)", example = "Password1!")
        @NotBlank @Size(min = 8, max = 15) String password,
        @Schema(description = "권한 (CUSTOMER | OWNER)", example = "CUSTOMER")
        @NotNull UserRole role
) {

}
