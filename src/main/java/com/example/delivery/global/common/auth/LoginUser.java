package com.example.delivery.global.common.auth;

import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import java.util.UUID;

public record LoginUser(UUID id, String username, UserRole role) {

    public static LoginUser from(UserEntity user) {
        return new LoginUser(user.getId(), user.getUsername().value(), user.getRole());
    }

    public boolean isSelf(String targetUsername) {
        return username.equals(targetUsername);
    }

    public boolean isManagerOrMaster() {
        return role == UserRole.MANAGER || role == UserRole.MASTER;
    }

    public boolean isMaster() {
        return role == UserRole.MASTER;
    }
}
