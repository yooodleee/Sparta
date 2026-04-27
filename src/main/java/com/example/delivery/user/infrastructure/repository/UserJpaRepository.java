package com.example.delivery.user.infrastructure.repository;

import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.vo.Username;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository
        extends JpaRepository<UserEntity, UUID>,
        JpaSpecificationExecutor<UserEntity> {

    @Query("select u from UserEntity u where u.username = :username")
    Optional<UserEntity> findByUsername(Username username);

    // soft-deleted row 까지 포함 — @SQLRestriction 우회를 위해 native query 사용.
    // username/email 의 영구 점유 정책을 강제하기 위함.

    @Query(value = "SELECT EXISTS(SELECT 1 FROM p_user WHERE username = :username)",
            nativeQuery = true)
    boolean existsByUsernameIncludingDeleted(@Param("username") String username);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM p_user WHERE email = :email)",
            nativeQuery = true)
    boolean existsByEmailIncludingDeleted(@Param("email") String email);

    @Query(value = """
            SELECT EXISTS(
                SELECT 1 FROM p_user
                WHERE email = :email
                  AND username <> :excludeUsername
            )
            """, nativeQuery = true)
    boolean existsByEmailExceptIncludingDeleted(
            @Param("email") String email,
            @Param("excludeUsername") String excludeUsername);
}
