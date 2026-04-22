package com.example.delivery.user.application.service;

import com.example.delivery.global.common.auth.LoginUser;
import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.global.common.exception.ResourceNotFoundException;
import com.example.delivery.global.common.response.PageResponse;
import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.user.domain.policy.PasswordPolicy;
import com.example.delivery.user.domain.repository.UserRepository;
import com.example.delivery.user.domain.vo.Email;
import com.example.delivery.user.presentation.dto.request.ReqChangeRole;
import com.example.delivery.user.presentation.dto.request.ReqUpdateUser;
import com.example.delivery.user.presentation.dto.response.ResUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Transactional
    public void softDelete(String username, LoginUser me) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        if (!me.isManagerOrMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (me.isSelf(username)) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_SELF);
        }
        if (user.getRole() == UserRole.MANAGER && !me.isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        user.softDelete(me.username());
    }

    @Transactional(readOnly = true)
    public ResUserDto getOne(String username, LoginUser me) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        if (!me.isSelf(username) && !me.isManagerOrMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return ResUserDto.from(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<ResUserDto> search(String keyword, UserRole role,
                                           Pageable pageable, LoginUser me) {
        if (!me.isManagerOrMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Page<ResUserDto> page = userRepository.search(keyword, role, pageable)
                .map(ResUserDto::from);
        return PageResponse.from(page);
    }

    @Transactional
    public ResUserDto changeRole(String username, ReqChangeRole req, LoginUser me) {
        if (!me.isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        // MASTER가 자기 자신의 권한을 변경하면 시스템이 MASTER 없이 남을 수 있으므로 차단한다.
        if (me.isSelf(username)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        user.changeRole(req.role());
        return ResUserDto.from(user);
    }
}
