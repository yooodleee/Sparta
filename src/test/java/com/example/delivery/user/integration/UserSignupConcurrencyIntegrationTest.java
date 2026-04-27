package com.example.delivery.user.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.user.domain.repository.UserRepository;
import com.example.delivery.user.presentation.dto.request.ReqSignup;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Signup 동시성 — 동일 username / email 경쟁 조건.
 *
 * 시나리오 문서: docs/test-scenarios-user-auth.md
 *
 * - {@code @Transactional} 롤백 사용 금지 — 각 요청이 별도 트랜잭션이어야 race 가 의미 있음.
 * - 매 실행마다 randomized prefix 로 격리, {@code @AfterEach} 에서 잔여 row 수동 정리.
 * - DB 는 application-test.yml 의 H2(PostgreSQL 모드) 를 그대로 사용한다.
 *   H2 도 {@code @Column(unique = true)} 의 unique constraint 를 INSERT 시점에 강제하므로
 *   "1 승, N-1 패" 분포 검증에는 충분하다 (운영 DB 와 100% 동일하지는 않으나 race 안전망 회귀 검증 목적).
 *
 * 정책 (영구 점유 / 거부):
 * - {@code AuthService.signup} 에 race 안전망(saveAndFlush + DataIntegrityViolation catch) 적용 →
 *   실패 응답은 모두 409 로 정규화되어야 한다. 5xx 가 발생하면 회귀.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserSignupConcurrencyIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;
    @Autowired
    UserRepository userRepository;
    @Autowired
    EntityManager entityManager;
    @Autowired
    PlatformTransactionManager txManager;

    @AfterEach
    void cleanup() {
        // @AfterEach 에는 Spring 의 @Transactional 이 적용되지 않으므로 TransactionTemplate 으로 수동 처리.
        new TransactionTemplate(txManager).executeWithoutResult(status ->
                entityManager.createNativeQuery("DELETE FROM p_user WHERE username LIKE 'race%'")
                        .executeUpdate());
    }

    @Test
    @DisplayName("동일 username 동시 signup → 성공 정확히 1, 살아있는 row 정확히 1")
    void concurrentSignup_sameUsername_exactlyOneSucceeds() throws Exception {
        int n = 8;
        String sharedUsername = "race" + (System.nanoTime() % 100_000);

        Outcome out = race(n, idx -> new ReqSignup(
                sharedUsername,
                "nick" + idx,
                "race" + idx + "_" + System.nanoTime() + "@example.com",
                "Abcd1234!",
                UserRole.CUSTOMER));

        assertThat(out.created.get()).as("성공 정확히 1개").isEqualTo(1);
        assertThat(out.conflict.get()).as("실패 N-1 개 모두 409").isEqualTo(n - 1);
        assertThat(out.serverError.get()).as("5xx 0 개").isZero();
        assertThat(out.other.get()).as("예측되지 않은 상태 코드 0").isZero();
        assertThat(userRepository.findByUsername(sharedUsername))
                .as("살아있는 row 1개").isPresent();
    }

    @Test
    @DisplayName("동일 email 동시 signup → 성공 정확히 1, 살아있는 row 정확히 1")
    void concurrentSignup_sameEmail_exactlyOneSucceeds() throws Exception {
        int n = 8;
        String sharedEmail = "race" + (System.nanoTime() % 100_000) + "@example.com";

        Outcome out = race(n, idx -> new ReqSignup(
                "race" + idx + (System.nanoTime() % 100_000),
                "nick" + idx,
                sharedEmail,
                "Abcd1234!",
                UserRole.CUSTOMER));

        assertThat(out.created.get()).as("성공 정확히 1개").isEqualTo(1);
        assertThat(out.conflict.get()).as("실패 N-1 개 모두 409").isEqualTo(n - 1);
        assertThat(out.serverError.get()).as("5xx 0 개").isZero();
        assertThat(out.other.get()).isZero();

        Long alive = new TransactionTemplate(txManager).execute(status -> ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM p_user WHERE email = :e AND deleted_at IS NULL")
                .setParameter("e", sharedEmail)
                .getSingleResult()).longValue());
        assertThat(alive).isEqualTo(1L);
    }

    // ────────────────────────────────────────────────
    // race 실행 헬퍼
    // ────────────────────────────────────────────────

    private Outcome race(int n, RequestFactory factory) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(n);
        CyclicBarrier barrier = new CyclicBarrier(n);

        Outcome out = new Outcome();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                    ReqSignup body = factory.build(idx);
                    ResponseEntity<String> res = restTemplate.postForEntity(
                            "/api/v1/auth/signup", body, String.class);
                    int code = res.getStatusCode().value();
                    if (code == 201) {
                        out.created.incrementAndGet();
                    } else if (code == 409) {
                        out.conflict.incrementAndGet();
                    } else if (code >= 500) {
                        out.serverError.incrementAndGet();
                    } else {
                        out.other.incrementAndGet();
                    }
                } catch (Exception e) {
                    out.other.incrementAndGet();
                }
            }));
        }
        for (Future<?> f : futures) {
            f.get(15, TimeUnit.SECONDS);
        }
        executor.shutdown();
        return out;
    }

    private static final class Outcome {
        final AtomicInteger created = new AtomicInteger();
        final AtomicInteger conflict = new AtomicInteger();
        final AtomicInteger serverError = new AtomicInteger();
        final AtomicInteger other = new AtomicInteger();
    }

    @FunctionalInterface
    private interface RequestFactory {
        ReqSignup build(int idx);
    }
}
