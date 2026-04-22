package com.example.delivery.user.application.service;

import com.example.delivery.global.common.auth.LoginUser;
import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.global.common.exception.ResourceNotFoundException;
import com.example.delivery.global.common.response.PageResponse;
import com.example.delivery.user.domain.command.UserUpdateCommand;
import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.user.domain.policy.PasswordPolicy;
import com.example.delivery.user.domain.repository.UserRepository;
import com.example.delivery.user.domain.vo.Email;
import com.example.delivery.user.presentation.dto.request.ReqChangeRole;
import com.example.delivery.user.presentation.dto.request.ReqUpdateUser;
import com.example.delivery.user.presentation.dto.response.ResUserDto;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
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
        UserEntity user = findUserOrThrow(username);
        user.update(me, toUpdateCommand(req, username));

        return ResUserDto.from(user);
    }

    @Transactional
    public void softDelete(String username, LoginUser me) {
        findUserOrThrow(username).deleteBy(me);
    }

    @Transactional(readOnly = true)
    public ResUserDto getOne(String username, LoginUser me) {
        UserEntity user = findUserOrThrow(username);
        user.assertReadableBy(me);

        return ResUserDto.from(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<ResUserDto> search(String keyword, UserRole role,
            Pageable pageable, LoginUser me) {
        if (!me.isManagerOrMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return PageResponse.from(
                userRepository.search(keyword, role, pageable).map(ResUserDto::from));
    }

    @Transactional
    public ResUserDto changeRole(String username, ReqChangeRole req, LoginUser me) {
        UserEntity user = findUserOrThrow(username);
        user.changeRoleBy(me, req.role());

        return ResUserDto.from(user);
    }

    private UserEntity findUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    private UserUpdateCommand toUpdateCommand(ReqUpdateUser req, String targetUsername) {
        Optional<Email> email = Optional.ofNullable(req.email())
                .map(Email::new)
                .map(value -> requireEmailAvailable(value, targetUsername));
        Optional<String> newPasswordHash = Optional.ofNullable(req.password())
                .map(this::encodeValidPassword);

        return new UserUpdateCommand(
                Optional.ofNullable(req.nickname()),
                email,
                newPasswordHash,
                Optional.ofNullable(req.isPublic()));
    }

    private Email requireEmailAvailable(Email email, String targetUsername) {
        if (userRepository.existsByEmailExcept(email.value(), targetUsername)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        return email;
    }

    private String encodeValidPassword(String raw) {
        PasswordPolicy.validate(raw);

        return passwordEncoder.encode(raw);
    }
}
