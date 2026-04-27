package com.example.delivery.user.integration;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.delivery.global.test.IntegrationTestSupport;
import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.user.presentation.dto.request.ReqLogin;
import com.example.delivery.user.presentation.dto.request.ReqSignup;
import com.example.delivery.user.presentation.dto.request.ReqUpdateUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Soft Delete ↔ Unique 제약 상호작용.
 *
 * 시나리오 문서: docs/test-scenarios-user-auth.md
 *
 * 정책: 삭제된 username/email 은 영구 점유로 본다 (재점유 거부).
 * - {@code AuthService.signup} 과 {@code UserService.update} 는 application 체크에서
 *   {@code existsBy*IncludingDeleted} 로 soft-deleted row 까지 포함해 검증한다.
 * - race 로 application 체크 사이를 통과한 경우에도
 *   {@code DataIntegrityViolationException} 을 catch 해서 409 로 정규화한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserSoftDeleteIntegrationTest extends IntegrationTestSupport {

    // ────────────────────────────────────────────────
    // 삭제된 username 재가입
    // ────────────────────────────────────────────────

    @Test
    @DisplayName("삭제된 계정으로 로그인 → 401, 동일 username 재가입 → 409 DUPLICATE_USERNAME")
    void deletedUsername_loginBlockedAndReSignupRejected() throws Exception {
        seedUser("manager01", UserRole.MANAGER);
        seedUser("zedfx", UserRole.CUSTOMER);

        String mToken = login("manager01", DEFAULT_PASSWORD);
        authedDelete("/api/v1/users/zedfx", mToken).andExpect(status().isNoContent());

        // (a) 삭제된 계정으로 로그인 → 401 (findByUsername 이 SQLRestriction 으로 못 찾음)
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReqLogin("zedfx", DEFAULT_PASSWORD))))
                .andExpect(status().isUnauthorized());

        // (b) 동일 username 으로 재가입 시도 → 409 (영구 점유 정책)
        ReqSignup retry = new ReqSignup("zedfx", "zed-again", "zed2@example.com",
                DEFAULT_PASSWORD, UserRole.CUSTOMER);
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(retry)))
                .andExpect(status().isConflict());
    }

    // ────────────────────────────────────────────────
    // 삭제된 email 재사용 (타 사용자 수정)
    // ────────────────────────────────────────────────

    @Test
    @DisplayName("삭제된 사용자의 email 로 다른 사용자가 PATCH: 살아있을 때도 삭제 후에도 409")
    void deletedEmail_patchByOther() throws Exception {
        seedUser("manager01", UserRole.MANAGER);
        seedUser("zedfx", UserRole.CUSTOMER);
        seedUser("alice", UserRole.CUSTOMER);

        String aliceToken = login("alice", DEFAULT_PASSWORD);

        // (1) zed 살아있을 때: 409 DUPLICATE_EMAIL
        authedPatch("/api/v1/users/alice", aliceToken,
                new ReqUpdateUser(null, "zedfx@example.com", null, null, null))
                .andExpect(status().isConflict());

        // (2) zed soft delete
        String mToken = login("manager01", DEFAULT_PASSWORD);
        authedDelete("/api/v1/users/zedfx", mToken).andExpect(status().isNoContent());

        // (3) zed 삭제 후 동일 시도 → 409 (영구 점유 정책)
        authedPatch("/api/v1/users/alice", aliceToken,
                new ReqUpdateUser(null, "zedfx@example.com", null, null, null))
                .andExpect(status().isConflict());
    }

    // ────────────────────────────────────────────────
    // 삭제된 email 로 신규 signup
    // ────────────────────────────────────────────────

    @Test
    @DisplayName("삭제된 사용자의 email 로 신규 signup → 409 DUPLICATE_EMAIL")
    void deletedEmail_newSignup() throws Exception {
        seedUser("manager01", UserRole.MANAGER);
        seedUser("zedfx", UserRole.CUSTOMER);

        String mToken = login("manager01", DEFAULT_PASSWORD);
        authedDelete("/api/v1/users/zedfx", mToken).andExpect(status().isNoContent());

        ReqSignup req = new ReqSignup("zed2", "zed2nick", "zedfx@example.com",
                DEFAULT_PASSWORD, UserRole.CUSTOMER);
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }
}
