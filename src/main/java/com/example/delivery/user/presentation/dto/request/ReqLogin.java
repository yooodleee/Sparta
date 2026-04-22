package com.example.delivery.user.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReqLogin(
        @NotBlank String username,
        @NotBlank String password
) {
}
