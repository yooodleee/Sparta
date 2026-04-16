package com.example.delivery.user.presentation.dto.response;

import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;

public record ResUserDto(
        String username,
        String nickname,
        String email,
        UserRole role
) {

    public static ResUserDto from(UserEntity user) {
        return new ResUserDto(user.getUsername(), user.getNickname(), user.getEmail(), user.getRole());
    }
}
