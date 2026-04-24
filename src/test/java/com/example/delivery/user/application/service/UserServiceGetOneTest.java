package com.example.delivery.user.application.service;

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
class UserServiceGetOneTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @InjectMocks
    UserService userService;

    private UserEntity user(String username, UserRole role) {
        return UserEntity.register(
                new Username(username), username,
                new Email(username + "@example.com"), "hash", role);
    }

    @Test
    @DisplayName("본인 조회 성공")
    void self_canRead() {
        UserEntity target = user("alice", UserRole.CUSTOMER);
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(target));
        LoginUser me = new LoginUser(UUID.randomUUID(), "alice", UserRole.CUSTOMER);

        assertThatCode(() -> userService.getOne("alice", me)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("MANAGER가 CUSTOMER 조회 성공")
    void managerReadsCustomer() {
        UserEntity target = user("alice", UserRole.CUSTOMER);
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(target));
        LoginUser mgr = new LoginUser(UUID.randomUUID(), "mgr1", UserRole.MANAGER);

        assertThatCode(() -> userService.getOne("alice", mgr)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("MANAGER가 다른 MANAGER 조회 시도 시 403")
    void managerCannotReadOtherManager() {
        UserEntity target = user("momo", UserRole.MANAGER);
        given(userRepository.findByUsername("momo")).willReturn(Optional.of(target));
        LoginUser other = new LoginUser(UUID.randomUUID(), "mgr2", UserRole.MANAGER);

        assertThatThrownBy(() -> userService.getOne("momo", other))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("MANAGER가 MASTER 조회 시도 시 403")
    void managerCannotReadMaster() {
        UserEntity target = user("root", UserRole.MASTER);
        given(userRepository.findByUsername("root")).willReturn(Optional.of(target));
        LoginUser mgr = new LoginUser(UUID.randomUUID(), "mgr1", UserRole.MANAGER);

        assertThatThrownBy(() -> userService.getOne("root", mgr))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("MASTER가 MANAGER 조회 성공")
    void masterReadsManager() {
        UserEntity target = user("momo", UserRole.MANAGER);
        given(userRepository.findByUsername("momo")).willReturn(Optional.of(target));
        LoginUser master = new LoginUser(UUID.randomUUID(), "root", UserRole.MASTER);

        assertThatCode(() -> userService.getOne("momo", master)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CUSTOMER가 타인 조회 시도 시 403")
    void customerCannotReadOther() {
        UserEntity target = user("alice", UserRole.CUSTOMER);
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(target));
        LoginUser other = new LoginUser(UUID.randomUUID(), "bob", UserRole.CUSTOMER);

        assertThatThrownBy(() -> userService.getOne("alice", other))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }
}
