package com.example.delivery.user.application.service;

import com.example.delivery.global.infrastructure.security.JwtTokenProvider;
import com.example.delivery.global.infrastructure.security.UserPrincipal;
import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.user.domain.repository.UserRepository;
import com.example.delivery.user.presentation.dto.response.ResCreateTestUserDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테스트 전용 서비스.
 * JWT 보호 엔드포인트 동작 검증을 위해 파라미터 없이 임시 사용자 + 토큰을 발급한다.
 * 실제 회원가입/로그인 플로우가 붙으면 이 클래스와 관련 컨트롤러/DTO는 제거한다.
 */
@Service
@RequiredArgsConstructor
public class TestAuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public ResCreateTestUserDto createDefaultAndIssueToken() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "t" + suffix;
        UserEntity saved = userRepository.save(UserEntity.builder()
                .username(username)
                .nickname("테스트" + suffix)
                .email(username + "@test.local")
                .role(UserRole.MASTER)
                .build());

        String token = jwtTokenProvider.issue(
                new UserPrincipal(saved.getId(), saved.getUsername(), saved.getRole()));
        return ResCreateTestUserDto.of(saved, token);
    }
}
