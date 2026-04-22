package com.example.delivery.user.presentation.dto.response;

import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import java.time.LocalDateTime;

public record ResSignup(
        String username,
        String nickname,
        String email,
        UserRole role,
        LocalDateTime createdAt
) {

    public static ResSignup from(UserEntity user) {
        return new ResSignup(
                user.getUsername().value(),
                user.getNickname(),
                user.getEmail().value(),
                user.getRole(),
                user.getCreatedAt());
    }
}
