package com.example.delivery.user.domain.repository;

import com.example.delivery.user.domain.entity.UserEntity;
import java.util.Optional;

public interface UserRepository {

    UserEntity save(UserEntity user);

    Optional<UserEntity> findById(Long id);

    Optional<UserEntity> findByUsername(String username);
}
