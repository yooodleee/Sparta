package com.example.delivery.user.domain.repository;

import com.example.delivery.user.domain.entity.UserEntity;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    UserEntity save(UserEntity user);

    Optional<UserEntity> findById(UUID id);

    Optional<UserEntity> findByUsername(String username);
}
