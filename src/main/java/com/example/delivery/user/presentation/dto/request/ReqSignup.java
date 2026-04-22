package com.example.delivery.user.presentation.dto.request;

import com.example.delivery.user.domain.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReqSignup(
        @NotBlank @Size(min = 4, max = 10) String username,
        @NotBlank @Size(max = 100) String nickname,
        @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 15) String password,
        @NotNull UserRole role
) {

}
