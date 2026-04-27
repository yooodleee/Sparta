package com.example.delivery.user.domain.repository;

import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository {

    UserEntity save(UserEntity user);

    Optional<UserEntity> findById(UUID id);

    Optional<UserEntity> findByUsername(String username);

    // soft-deleted 까지 포함하여 점유 여부를 확인한다 (영구 점유 정책 강제용).

    boolean existsByUsernameIncludingDeleted(String username);

    boolean existsByEmailIncludingDeleted(String email);

    boolean existsByEmailExceptIncludingDeleted(String email, String excludeUsername);

    Page<UserEntity> search(String keyword, UserRole role, Pageable pageable);
}
