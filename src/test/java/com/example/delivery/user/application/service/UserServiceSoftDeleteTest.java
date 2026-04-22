package com.example.delivery.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.delivery.global.common.auth.LoginUser;
import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.user.domain.repository.UserRepository;
import com.example.delivery.user.domain.vo.Email;
import com.example.delivery.user.domain.vo.Username;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceSoftDeleteTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @InjectMocks
    UserService userService;

    UserEntity customer() {
        return UserEntity.register(new Username("alice"), "Alice",
                new Email("alice@example.com"), "hash", UserRole.CUSTOMER);
    }

    UserEntity manager() {
        return UserEntity.register(new Username("momo"), "Mo",
                new Email("mo@example.com"), "hash", UserRole.MANAGER);
    }

    @Test
    @DisplayName("본인 자기 삭제는 400 CANNOT_DELETE_SELF")
    void selfDelete_blocked() {
        UserEntity target = manager();
        given(userRepository.findByUsername("momo")).willReturn(Optional.of(target));
        LoginUser me = new LoginUser(UUID.randomUUID(), "momo", UserRole.MANAGER);

        assertThatThrownBy(() -> userService.softDelete("momo", me))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_DELETE_SELF);
    }

    @Test
    @DisplayName("MANAGER가 MANAGER 삭제 시도 시 403")
    void managerCannotDeleteManager() {
        UserEntity target = manager();
        given(userRepository.findByUsername("momo")).willReturn(Optional.of(target));
        LoginUser other = new LoginUser(UUID.randomUUID(), "mgr2", UserRole.MANAGER);

        assertThatThrownBy(() -> userService.softDelete("momo", other))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("MASTER가 MANAGER 삭제 성공 (deletedAt 세팅)")
    void masterDeletesManager() {
        UserEntity target = manager();
        given(userRepository.findByUsername("momo")).willReturn(Optional.of(target));
        LoginUser master = new LoginUser(UUID.randomUUID(), "root", UserRole.MASTER);

        assertThatCode(() -> userService.softDelete("momo", master)).doesNotThrowAnyException();
        assertThat(target.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("MANAGER가 CUSTOMER 삭제 성공")
    void managerDeletesCustomer() {
        UserEntity target = customer();
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(target));
        LoginUser mgr = new LoginUser(UUID.randomUUID(), "mgr1", UserRole.MANAGER);

        assertThatCode(() -> userService.softDelete("alice", mgr)).doesNotThrowAnyException();
        assertThat(target.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("CUSTOMER가 타인 삭제 시도 시 403")
    void customerForbidden() {
        UserEntity target = customer();
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(target));
        LoginUser other = new LoginUser(UUID.randomUUID(), "bob", UserRole.CUSTOMER);

        assertThatThrownBy(() -> userService.softDelete("alice", other))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }
}
