package com.example.delivery.user.presentation.dto.response;

import com.example.delivery.user.domain.entity.UserRole;

public record ResLogin(
        String accessToken,
        String username,
        UserRole role
) {
}
