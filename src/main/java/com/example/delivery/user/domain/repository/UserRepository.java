package com.example.delivery.user.domain.repository;

import com.example.delivery.user.domain.entity.UserEntity;

import java.util.Optional;

public interface UserRepository {

    Optional<UserEntity> findByUsername(String username);
}
