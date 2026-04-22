package com.example.delivery.user.presentation.dto.request;

import com.example.delivery.user.domain.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record ReqChangeRole(@NotNull UserRole role) {

}
