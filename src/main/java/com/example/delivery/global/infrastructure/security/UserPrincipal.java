package com.example.delivery.global.infrastructure.security;

import com.example.delivery.user.domain.entity.UserRole;
import org.springframework.security.core.AuthenticatedPrincipal;

public record UserPrincipal(
        Long id,
        String username,
        UserRole role
) implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return username;
    }

    public String authority() {
        return "ROLE_" + role.name();
    }
}
