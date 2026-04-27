package com.example.delivery.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.example.delivery.global.common.auth.LoginUser;
import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.user.domain.repository.UserRepository;
import com.example.delivery.user.domain.vo.Email;
import com.example.delivery.user.domain.vo.Username;
import com.example.delivery.user.presentation.dto.request.ReqUpdateUser;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceUpdateTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @InjectMocks
    UserService userService;

    UserEntity target;

    @BeforeEach
    void setUp() {
        target = UserEntity.register(
                new Username("alice"),
                "Alice",
                new Email("alice@example.com"),
                "hash",
                UserRole.CUSTOMER);
        lenient().when(userRepository.findByUsername("alice")).thenReturn(Optional.of(target));
    }

    @Test
    @DisplayName("본인은 nickname/email/password/isPublic 모두 수정 가능 (currentPassword 일치)")
    void self_canUpdateAll() {
        given(userRepository.existsByEmailExceptIncludingDeleted(any(), any())).willReturn(false);
        given(passwordEncoder.matches("OldPass1!", "hash")).willReturn(true);
        given(passwordEncoder.encode("Abcd1234!")).willReturn("new-hash");

        ReqUpdateUser req = new ReqUpdateUser(
                "NewNick", "new@mail.co", "Abcd1234!", "OldPass1!", false);
        LoginUser me = new LoginUser(UUID.randomUUID(), "alice", UserRole.CUSTOMER);

        assertThatCode(() -> userService.update("alice", req, me)).doesNotThrowAnyException();

        assertThat(target.getNickname()).isEqualTo("NewNick");
        assertThat(target.getEmail().value()).isEqualTo("new@mail.co");
        assertThat(target.getPasswordHash()).isEqualTo("new-hash");
        assertThat(target.isPublic()).isFalse();
        verify(passwordEncoder).encode("Abcd1234!");
    }

    @Test
    @DisplayName("본인이 password 변경 시 currentPassword 누락 → 400")
    void self_passwordChange_missingCurrent() {
        ReqUpdateUser req = new ReqUpdateUser(null, null, "Abcd1234!", null, null);
        LoginUser me = new LoginUser(UUID.randomUUID(), "alice", UserRole.CUSTOMER);

        assertThatThrownBy(() -> userService.update("alice", req, me))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CURRENT_PASSWORD_REQUIRED);
        assertThat(target.getPasswordHash()).isEqualTo("hash");
    }

    @Test
    @DisplayName("본인이 password 변경 시 currentPassword 불일치 → 401")
    void self_passwordChange_mismatch() {
        given(passwordEncoder.matches("WrongOld1!", "hash")).willReturn(false);

        ReqUpdateUser req = new ReqUpdateUser(null, null, "Abcd1234!", "WrongOld1!", null);
        LoginUser me = new LoginUser(UUID.randomUUID(), "alice", UserRole.CUSTOMER);

        assertThatThrownBy(() -> userService.update("alice", req, me))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CURRENT_PASSWORD);
        assertThat(target.getPasswordHash()).isEqualTo("hash");
    }

    @Test
    @DisplayName("password 없는 본인 업데이트는 currentPassword 없이도 통과")
    void self_updateWithoutPassword_doesNotRequireCurrent() {
        ReqUpdateUser req = new ReqUpdateUser("NewNick", null, null, null, null);
        LoginUser me = new LoginUser(UUID.randomUUID(), "alice", UserRole.CUSTOMER);

        assertThatCode(() -> userService.update("alice", req, me)).doesNotThrowAnyException();
        assertThat(target.getNickname()).isEqualTo("NewNick");
    }

    @Test
    @DisplayName("MANAGER는 타인 수정 가능하나 password 수정 시도는 403")
    void manager_blockedFromPassword() {
        given(passwordEncoder.encode("Abcd1234!")).willReturn("enc");
        ReqUpdateUser req = new ReqUpdateUser("x", null, "Abcd1234!", null, null);
        LoginUser me = new LoginUser(UUID.randomUUID(), "bob", UserRole.MANAGER);

        assertThatThrownBy(() -> userService.update("alice", req, me))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("MANAGER가 다른 MANAGER 수정 시도 시 403")
    void manager_cannotUpdateOtherManager() {
        UserEntity manager = UserEntity.register(
                new Username("momo"), "Mo",
                new Email("mo@example.com"), "hash", UserRole.MANAGER);
        given(userRepository.findByUsername("momo")).willReturn(Optional.of(manager));

        ReqUpdateUser req = new ReqUpdateUser("x", null, null, null, null);
        LoginUser other = new LoginUser(UUID.randomUUID(), "mgr2", UserRole.MANAGER);

        assertThatThrownBy(() -> userService.update("momo", req, other))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("MANAGER가 MASTER 수정 시도 시 403")
    void manager_cannotUpdateMaster() {
        UserEntity master = UserEntity.register(
                new Username("root"), "Root",
                new Email("root@example.com"), "hash", UserRole.MASTER);
        given(userRepository.findByUsername("root")).willReturn(Optional.of(master));

        ReqUpdateUser req = new ReqUpdateUser("x", null, null, null, null);
        LoginUser mgr = new LoginUser(UUID.randomUUID(), "mgr1", UserRole.MANAGER);

        assertThatThrownBy(() -> userService.update("root", req, mgr))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("MASTER는 MANAGER 수정 가능")
    void master_canUpdateManager() {
        UserEntity manager = UserEntity.register(
                new Username("momo"), "Mo",
                new Email("mo@example.com"), "hash", UserRole.MANAGER);
        given(userRepository.findByUsername("momo")).willReturn(Optional.of(manager));

        ReqUpdateUser req = new ReqUpdateUser("x", null, null, null, null);
        LoginUser master = new LoginUser(UUID.randomUUID(), "root", UserRole.MASTER);

        assertThatCode(() -> userService.update("momo", req, master)).doesNotThrowAnyException();
        assertThat(manager.getNickname()).isEqualTo("x");
    }

    @Test
    @DisplayName("MASTER는 타인 수정 가능 (password 제외)")
    void master_canUpdateNickname() {
        ReqUpdateUser req = new ReqUpdateUser("x", null, null, null, null);
        LoginUser me = new LoginUser(UUID.randomUUID(), "root", UserRole.MASTER);

        assertThatCode(() -> userService.update("alice", req, me)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("일반 타인(CUSTOMER)은 타인 수정 시 403")
    void other_forbidden() {
        ReqUpdateUser req = new ReqUpdateUser("x", null, null, null, null);
        LoginUser me = new LoginUser(UUID.randomUUID(), "bob", UserRole.CUSTOMER);

        assertThatThrownBy(() -> userService.update("alice", req, me))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("본인이 중복 email(다른 사용자 사용 중)로 수정 시 409")
    void self_duplicateEmail() {
        given(userRepository.existsByEmailExceptIncludingDeleted("taken@mail.co", "alice")).willReturn(true);
        ReqUpdateUser req = new ReqUpdateUser(null, "taken@mail.co", null, null, null);
        LoginUser me = new LoginUser(UUID.randomUUID(), "alice", UserRole.CUSTOMER);

        assertThatThrownBy(() -> userService.update("alice", req, me))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);
    }
}
