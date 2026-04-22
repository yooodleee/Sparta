package com.example.delivery.user.presentation.dto.response;

import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import java.util.UUID;

/**
 * 테스트 전용 응답 DTO. 실제 회원가입/로그인 응답 포맷이 정해지면 제거한다.
 */
public record ResCreateTestUserDto(
        UUID id,
        String username,
        String nickname,
        String email,
        UserRole role,
        String token
) {

    public static ResCreateTestUserDto of(UserEntity user, String token) {
        return new ResCreateTestUserDto(
                user.getId(),
                user.getUsername().value(),
                user.getNickname(),
                user.getEmail().value(),
                user.getRole(),
                token);
    }
}
