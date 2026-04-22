package com.example.delivery.user.domain.entity;

import com.example.delivery.global.common.auth.LoginUser;
import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.global.infrastructure.entity.BaseEntity;
import com.example.delivery.user.domain.command.UserUpdateCommand;
import com.example.delivery.user.domain.vo.Email;
import com.example.delivery.user.domain.vo.EmailConverter;
import com.example.delivery.user.domain.vo.Username;
import com.example.delivery.user.domain.vo.UsernameConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@Getter
@Table(name = "p_user")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 10)
    @Convert(converter = UsernameConverter.class)
    private Username username;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(nullable = false, unique = true, length = 255)
    @Convert(converter = EmailConverter.class)
    private Email email;

    @Column(name = "password", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    private UserEntity(Username username, String nickname, Email email,
            String passwordHash, UserRole role) {
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isPublic = true;
    }

    public static UserEntity register(Username username, String nickname, Email email,
            String passwordHash, UserRole role) {
        return new UserEntity(username, nickname, email, passwordHash, role);
    }

    public void update(LoginUser actor, UserUpdateCommand command) {
        boolean self = actor.isSelf(this.username.value());
        if (!self) {
            assertManageableByNonSelf(actor);
        }
        if (command.newPasswordHash().isPresent() && !self) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        command.nickname().ifPresent(value -> this.nickname = value);
        command.email().ifPresent(value -> this.email = value);
        command.newPasswordHash().ifPresent(value -> this.passwordHash = value);
        command.isPublic().ifPresent(value -> this.isPublic = value);
    }

    public void deleteBy(LoginUser actor) {
        if (actor.isSelf(this.username.value())) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_SELF);
        }
        assertManageableByNonSelf(actor);
        softDelete(actor.username());
    }

    public void changeRoleBy(LoginUser actor, UserRole newRole) {
        if (!actor.isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (actor.isSelf(this.username.value())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        this.role = newRole;
    }

    public void assertReadableBy(LoginUser actor) {
        if (actor.isSelf(this.username.value())) {
            return;
        }
        assertManageableByNonSelf(actor);
    }

    private void assertManageableByNonSelf(LoginUser actor) {
        if (!actor.isManagerOrMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (this.role.isPrivileged() && !actor.isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    public void verifyPassword(String raw, PasswordEncoder encoder) {
        if (!encoder.matches(raw, this.passwordHash)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
