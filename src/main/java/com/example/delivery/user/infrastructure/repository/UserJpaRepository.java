package com.example.delivery.user.infrastructure.repository;

import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.vo.Email;
import com.example.delivery.user.domain.vo.Username;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface UserJpaRepository
        extends JpaRepository<UserEntity, UUID>,
        JpaSpecificationExecutor<UserEntity> {

    @Query("select u from UserEntity u where u.username = :username")
    Optional<UserEntity> findByUsername(Username username);

    boolean existsByUsername(Username username);

    boolean existsByEmail(Email email);

    @Query("""
            select case when count(u) > 0 then true else false end
            from UserEntity u
            where u.email = :email
              and u.username <> :excludeUsername
            """)
    boolean existsByEmailExcept(Email email, Username excludeUsername);
}
