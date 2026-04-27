package com.example.delivery.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.delivery.global.test.IntegrationTestSupport;
import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.user.domain.vo.Username;
import com.example.delivery.user.presentation.dto.request.ReqChangeRole;
import com.example.delivery.user.presentation.dto.request.ReqUpdateUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Role 전이 체인과 JWT 일괄 무효화 + Privileged ↔ Privileged 교차 권한 매트릭스.
 *
 * 시나리오 문서: docs/test-scenarios-user-auth.md
 *
 * 시나리오 문서의 짧은 username (m1, m2, o1, c1) 은 Username VO 정책(소문자/숫자 4~10자)
 * 위반이라 실제 코드에서 거부된다. 본 테스트는 mngr1/mngr2/ownr1/cust1 로 치환하여 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserAuthRoleTransitionIntegrationTest extends IntegrationTestSupport {

    // ────────────────────────────────────────────────
    // Role 전이 체인
    // ────────────────────────────────────────────────

    @Test
    @DisplayName("role 전이 시 이전 JWT 즉시 무효화 → 재로그인 이후에만 신규 권한 적용")
    void roleTransition_invalidatesPreviousTokens() throws Exception {
        seedUser("master01", UserRole.MASTER);
        seedUser("cara01", UserRole.CUSTOMER);

        // step 1: cara01 로그인 → 토큰 A (role=CUSTOMER)
        String tokenA = login("cara01", DEFAULT_PASSWORD);

        // step 2: master01 이 cara01 → OWNER 로 변경
        String masterToken = login("master01", DEFAULT_PASSWORD);
        authedPatch("/api/v1/users/cara01/role", masterToken, new ReqChangeRole(UserRole.OWNER))
                .andExpect(status().isOk());

        // step 3: 토큰 A 로 /me → 403 ROLE_MISMATCH (JwtAuthenticationFilter)
        authedGet("/api/v1/users/me", tokenA)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("ROLE_MISMATCH"));

        // step 4~5: cara01 재로그인 → 토큰 B, /me 200 OWNER
        String tokenB = login("cara01", DEFAULT_PASSWORD);
        authedGet("/api/v1/users/me", tokenB)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("OWNER"));

        // step 6: master01 이 cara01 → MANAGER 로 변경
        authedPatch("/api/v1/users/cara01/role", masterToken, new ReqChangeRole(UserRole.MANAGER))
                .andExpect(status().isOk());

        // step 7: 토큰 B 로 /me → 403 ROLE_MISMATCH
        authedGet("/api/v1/users/me", tokenB)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("ROLE_MISMATCH"));

        // step 8: 토큰 B 로 /users (목록) → 403 ROLE_MISMATCH
        // (인가 단계가 아니라 필터 재검증 단계에서 막혀야 함)
        authedGet("/api/v1/users", tokenB)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("ROLE_MISMATCH"));

        // step 9~10: 재로그인 → 토큰 C (MANAGER), /users?role=CUSTOMER&size=10 → 200
        String tokenC = login("cara01", DEFAULT_PASSWORD);
        authedGet("/api/v1/users?role=CUSTOMER&size=10", tokenC)
                .andExpect(status().isOk());
    }

    // ────────────────────────────────────────────────
    // Privileged 매트릭스 — 시드는 매 테스트가 직접 수행
    // (DELETE 가 row 를 변형시키므로 grouping 별로 메서드 분리)
    // ────────────────────────────────────────────────

    @Test
    @DisplayName("read 매트릭스: MANAGER 는 비-privileged 와 본인 조회 가능, 다른 MANAGER/MASTER 는 403")
    void manager_readMatrix() throws Exception {
        seedUser("master01", UserRole.MASTER);
        seedUser("mngr1", UserRole.MANAGER);
        seedUser("mngr2", UserRole.MANAGER);
        seedUser("ownr1", UserRole.OWNER);
        seedUser("cust1", UserRole.CUSTOMER);

        String mngr1Token = login("mngr1", DEFAULT_PASSWORD);

        authedGet("/api/v1/users/cust1", mngr1Token).andExpect(status().isOk());
        authedGet("/api/v1/users/ownr1", mngr1Token).andExpect(status().isOk());
        authedGet("/api/v1/users/mngr1", mngr1Token).andExpect(status().isOk()); // self
        authedGet("/api/v1/users/mngr2", mngr1Token).andExpect(status().isForbidden());
        authedGet("/api/v1/users/master01", mngr1Token).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("mutate 매트릭스: MANAGER 가 다른 MANAGER/MASTER 의 nickname 수정 시도 → 403")
    void manager_cannotMutatePrivilegedPeer() throws Exception {
        seedUser("master01", UserRole.MASTER);
        seedUser("mngr1", UserRole.MANAGER);
        seedUser("mngr2", UserRole.MANAGER);

        String mngr1Token = login("mngr1", DEFAULT_PASSWORD);
        ReqUpdateUser body = new ReqUpdateUser("changed", null, null, null, null);

        authedPatch("/api/v1/users/mngr2", mngr1Token, body).andExpect(status().isForbidden());
        authedPatch("/api/v1/users/master01", mngr1Token, body).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("delete 매트릭스: MANAGER 는 다른 MANAGER 삭제 불가(403), OWNER 는 삭제 가능(204)")
    void manager_deleteMatrix_andSoftDeleteAudit() throws Exception {
        seedUser("master01", UserRole.MASTER);
        seedUser("mngr1", UserRole.MANAGER);
        seedUser("mngr2", UserRole.MANAGER);
        seedUser("ownr1", UserRole.OWNER);

        String mngr1Token = login("mngr1", DEFAULT_PASSWORD);

        authedDelete("/api/v1/users/mngr2", mngr1Token).andExpect(status().isForbidden());
        authedDelete("/api/v1/users/ownr1", mngr1Token).andExpect(status().isNoContent());

        // 스팟 체크: ownr1 이 soft delete 후 일반 조회로 더는 안 보임
        assertThat(userRepository.findByUsername("ownr1")).isEmpty();
    }

    @Test
    @DisplayName("role 변경: MANAGER 는 role 변경 불가 → 403 (MASTER 전용)")
    void manager_cannotChangeRole() throws Exception {
        seedUser("mngr1", UserRole.MANAGER);
        seedUser("cust1", UserRole.CUSTOMER);

        String mngr1Token = login("mngr1", DEFAULT_PASSWORD);

        authedPatch("/api/v1/users/cust1/role", mngr1Token, new ReqChangeRole(UserRole.OWNER))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MASTER 는 MANAGER 에 대한 GET / PATCH / DELETE 모두 가능")
    void master_fullCrudOnManager() throws Exception {
        seedUser("master01", UserRole.MASTER);
        UserEntity mngr1 = seedUser("mngr1", UserRole.MANAGER);

        String masterToken = login("master01", DEFAULT_PASSWORD);

        authedGet("/api/v1/users/mngr1", masterToken).andExpect(status().isOk());
        authedPatch("/api/v1/users/mngr1", masterToken,
                new ReqUpdateUser("byMaster", null, null, null, null))
                .andExpect(status().isOk());
        authedDelete("/api/v1/users/mngr1", masterToken).andExpect(status().isNoContent());

        assertThat(userRepository.findByUsername("mngr1")).isEmpty();
        assertThat(mngr1.getUsername()).isEqualTo(new Username("mngr1"));
    }
}
