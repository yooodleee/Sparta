package com.example.delivery.user.presentation.dto.response;

import com.example.delivery.global.infrastructure.security.UserPrincipal;
import com.example.delivery.user.domain.entity.UserRole;
import java.util.UUID;

/**
 * 테스트 전용 응답 DTO. {@code /api/v1/test/me}에서 인증된 principal을 그대로 비춰주기 위한 용도.
 * 실제 "내 정보 조회" 엔드포인트가 정식으로 정의되면 제거한다.
 */
public record ResTestMeDto(
        UUID id,
        String username,
        UserRole role
) {

    public static ResTestMeDto from(UserPrincipal principal) {
        return new ResTestMeDto(principal.id(), principal.username(), principal.role());
    }
}
