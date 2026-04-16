package com.example.delivery.user.application.service;

import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.global.common.exception.ResourceNotFoundException;
import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.repository.UserRepository;
import com.example.delivery.user.presentation.dto.response.ResUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceV1 {

    private final UserRepository userRepository;

    public ResUserDto findByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        return ResUserDto.from(user);
    }
}
