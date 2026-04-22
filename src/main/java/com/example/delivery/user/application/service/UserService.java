package com.example.delivery.user.application.service;

import com.example.delivery.global.common.auth.LoginUser;
import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.global.common.exception.ResourceNotFoundException;
import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.policy.PasswordPolicy;
import com.example.delivery.user.domain.repository.UserRepository;
import com.example.delivery.user.domain.vo.Email;
import com.example.delivery.user.presentation.dto.request.ReqUpdateUser;
import com.example.delivery.user.presentation.dto.response.ResUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ResUserDto update(String username, ReqUpdateUser req, LoginUser me) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        boolean self = me.isSelf(username);
        boolean privileged = me.isManagerOrMaster();
        if (!self && !privileged) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Email newEmail = null;
        if (req.email() != null) {
            newEmail = new Email(req.email());
            if (userRepository.existsByEmailExcept(newEmail.value(), username)) {
                throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
            }
        }

        if (self) {
            String newHash = null;
            if (req.password() != null) {
                PasswordPolicy.validate(req.password());
                newHash = passwordEncoder.encode(req.password());
            }
            user.updateBySelf(req.nickname(), newEmail, newHash, req.isPublic());
        } else {
            if (req.password() != null) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            user.updateByManager(req.nickname(), newEmail, req.isPublic());
        }
        return ResUserDto.from(user);
    }
}
