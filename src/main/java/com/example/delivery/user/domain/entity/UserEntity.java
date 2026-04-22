package com.example.delivery.user.domain.entity;

import com.example.delivery.global.infrastructure.entity.BaseEntity;
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

    public void updateBySelf(String nickname, Email email, String newPasswordHash, Boolean isPublic) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (email != null) {
            this.email = email;
        }
        if (newPasswordHash != null) {
            this.passwordHash = newPasswordHash;
        }
        if (isPublic != null) {
            this.isPublic = isPublic;
        }
    }

    public void updateByManager(String nickname, Email email, Boolean isPublic) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (email != null) {
            this.email = email;
        }
        if (isPublic != null) {
            this.isPublic = isPublic;
        }
    }

    public void changeRole(UserRole newRole) {
        this.role = newRole;
    }

    public boolean matchesPassword(String raw, PasswordEncoder encoder) {
        return encoder.matches(raw, this.passwordHash);
    }
}
