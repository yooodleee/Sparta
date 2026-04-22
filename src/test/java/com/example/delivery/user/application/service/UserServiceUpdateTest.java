package com.example.delivery.user.application.service;

import com.example.delivery.global.common.auth.LoginUser;
import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.user.domain.repository.UserRepository;
import com.example.delivery.user.domain.vo.Email;
import com.example.delivery.user.domain.vo.Username;
import com.example.delivery.user.presentation.dto.request.ReqUpdateUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceUpdateTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService userService;

    UserEntity target;

    @BeforeEach
    void setUp() {
        target = UserEntity.register(
                new Username("alice"),
                "Alice",
                new Email("alice@example.com"),
                "hash",
                UserRole.CUSTOMER);
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(target));
    }

    @Test
    @DisplayName("본인은 nickname/email/password/isPublic 모두 수정 가능")
    void self_canUpdateAll() {
        given(userRepository.existsByEmailExcept(any(), any())).willReturn(false);
        given(passwordEncoder.encode("Abcd1234!")).willReturn("new-hash");

        ReqUpdateUser req = new ReqUpdateUser("NewNick", "new@mail.co", "Abcd1234!", false);
        LoginUser me = new LoginUser(UUID.randomUUID(), "alice", UserRole.CUSTOMER);

        assertThatCode(() -> userService.update("alice", req, me)).doesNotThrowAnyException();

        assertThat(target.getNickname()).isEqualTo("NewNick");
        assertThat(target.getEmail().value()).isEqualTo("new@mail.co");
        assertThat(target.getPasswordHash()).isEqualTo("new-hash");
        assertThat(target.isPublic()).isFalse();
        verify(passwordEncoder).encode("Abcd1234!");
    }

    @Test
    @DisplayName("MANAGER는 타인 수정 가능하나 password 수정 시도는 403")
    void manager_blockedFromPassword() {
        ReqUpdateUser req = new ReqUpdateUser("x", null, "Abcd1234!", null);
        LoginUser me = new LoginUser(UUID.randomUUID(), "bob", UserRole.MANAGER);

        assertThatThrownBy(() -> userService.update("alice", req, me))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("MASTER는 타인 수정 가능 (password 제외)")
    void master_canUpdateNickname() {
        ReqUpdateUser req = new ReqUpdateUser("x", null, null, null);
        LoginUser me = new LoginUser(UUID.randomUUID(), "root", UserRole.MASTER);

        assertThatCode(() -> userService.update("alice", req, me)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("일반 타인(CUSTOMER)은 타인 수정 시 403")
    void other_forbidden() {
        ReqUpdateUser req = new ReqUpdateUser("x", null, null, null);
        LoginUser me = new LoginUser(UUID.randomUUID(), "bob", UserRole.CUSTOMER);

        assertThatThrownBy(() -> userService.update("alice", req, me))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("본인이 중복 email(다른 사용자 사용 중)로 수정 시 409")
    void self_duplicateEmail() {
        given(userRepository.existsByEmailExcept("taken@mail.co", "alice")).willReturn(true);
        ReqUpdateUser req = new ReqUpdateUser(null, "taken@mail.co", null, null);
        LoginUser me = new LoginUser(UUID.randomUUID(), "alice", UserRole.CUSTOMER);

        assertThatThrownBy(() -> userService.update("alice", req, me))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);
    }
}
