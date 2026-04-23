package com.example.delivery.user.presentation.dto.response;

import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import java.time.LocalDateTime;

public record ResUserDto(
        String username,
        String nickname,
        String email,
        UserRole role,
        boolean isPublic,
        LocalDateTime createdAt
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
