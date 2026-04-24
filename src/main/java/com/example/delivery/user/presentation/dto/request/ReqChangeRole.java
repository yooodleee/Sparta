package com.example.delivery.user.presentation.dto.request;

import com.example.delivery.user.domain.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "권한 변경 요청")
public record ReqChangeRole(
        @Schema(description = "새 권한", example = "MANAGER")
        @NotNull UserRole role) {

}
