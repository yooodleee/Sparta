package com.example.delivery.user.application.service;

import com.example.delivery.global.common.auth.LoginUser;
import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.global.infrastructure.security.JwtTokenProvider;
import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.policy.PasswordPolicy;
import com.example.delivery.user.domain.repository.UserRepository;
import com.example.delivery.user.domain.vo.Email;
import com.example.delivery.user.domain.vo.Username;
import com.example.delivery.user.presentation.dto.request.ReqLogin;
import com.example.delivery.user.presentation.dto.request.ReqSignup;
import com.example.delivery.user.presentation.dto.response.ResLogin;
import com.example.delivery.user.presentation.dto.response.ResSignup;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public ResSignup signup(ReqSignup req) {
        if (!req.role().isSignupAllowed()) {
            throw new BusinessException(ErrorCode.SIGNUP_ROLE_NOT_ALLOWED);
        }
        PasswordPolicy.validate(req.password());
        Username username = new Username(req.username());
        Email email = new Email(req.email());

        requireUsernameAvailable(username);
        requireEmailAvailable(email);

        // race 안전망(unique 보장)은 UserRepository 가 책임진다.
        UserEntity saved = userRepository.save(UserEntity.register(
                username, req.nickname(), email,
                passwordEncoder.encode(req.password()), req.role()));
        return ResSignup.from(saved);
    }

    @Transactional(readOnly = true)
    public ResLogin login(ReqLogin req) {
        UserEntity user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        user.verifyPassword(req.password(), passwordEncoder);
        String token = jwtTokenProvider.issue(LoginUser.from(user));

        return ResLogin.of(token, user);
    }

    private void requireUsernameAvailable(Username username) {
        // soft-deleted 까지 포함하여 영구 점유로 본다.
        if (userRepository.existsByUsernameIncludingDeleted(username.value())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
    }

    private void requireEmailAvailable(Email email) {
        if (userRepository.existsByEmailIncludingDeleted(email.value())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
    }
}
