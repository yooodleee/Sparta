package com.example.delivery.user.presentation.dto.response;

import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;

public record ResLogin(
        String accessToken,
        String username,
        UserRole role
) {

    public static ResLogin of(String accessToken, UserEntity user) {
        return new ResLogin(accessToken, user.getUsername().value(), user.getRole());
    }
}
