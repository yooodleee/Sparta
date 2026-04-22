package com.example.delivery.user.infrastructure.repository;

import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.user.domain.repository.UserRepository;
import com.example.delivery.user.domain.repository.UserSearchSpecs;
import com.example.delivery.user.domain.vo.Email;
import com.example.delivery.user.domain.vo.Username;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public UserEntity save(UserEntity user) {
        return userJpaRepository.save(user);
    }

    @Override
    public Optional<UserEntity> findById(UUID id) {
        return userJpaRepository.findById(id);
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return userJpaRepository.findByUsername(new Username(username));
    }

    @Override
    public boolean existsByUsername(String username) {
        return userJpaRepository.existsByUsername(new Username(username));
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(new Email(email));
    }

    @Override
    public boolean existsByEmailExcept(String email, String excludeUsername) {
        return userJpaRepository.existsByEmailExcept(new Email(email), new Username(excludeUsername));
    }

    @Override
    public Page<UserEntity> search(String keyword, UserRole role, Pageable pageable) {
        return userJpaRepository.findAll(UserSearchSpecs.build(keyword, role), pageable);
    }
}
