package com.example.delivery.user.infrastructure.repository;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.user.domain.repository.UserRepository;
import com.example.delivery.user.domain.repository.UserSearchSpecs;
import com.example.delivery.user.domain.vo.Username;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public UserEntity save(UserEntity user) {
        // unique 보장은 repository 책임. 즉시 flush 해서 violation 을 도메인 예외로 정규화한다.
        try {
            return userJpaRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw UniqueViolation.translate(e);
        }
    }

    @Override
    public Optional<UserEntity> findById(UUID id) {
        return userJpaRepository.findById(id);
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return userJpaRepository.findByUsername(new Username(username));
    }

    @Override
    public boolean existsByUsernameIncludingDeleted(String username) {
        return userJpaRepository.existsByUsernameIncludingDeleted(username);
    }

    @Override
    public boolean existsByEmailIncludingDeleted(String email) {
        return userJpaRepository.existsByEmailIncludingDeleted(email);
    }

    @Override
    public boolean existsByEmailExceptIncludingDeleted(String email, String excludeUsername) {
        return userJpaRepository.existsByEmailExceptIncludingDeleted(email, excludeUsername);
    }

    @Override
    public Page<UserEntity> search(String keyword, UserRole role, Pageable pageable) {
        return userJpaRepository.findAll(UserSearchSpecs.build(keyword, role), pageable);
    }

    /**
     * p_user 의 unique 컬럼별 도메인 예외 매핑.
     * - 새 unique 컬럼 추가 시 enum 한 줄만 추가
     * - 매칭 우선순위: USERNAME 먼저 (가입 race 의 대다수)
     * - SQL statement 절단으로 컬럼 리스트 노이즈 제거 (다른 컬럼명 오매칭 방지)
     */
    private enum UniqueViolation {
        USERNAME("USERNAME", ErrorCode.DUPLICATE_USERNAME),
        EMAIL("EMAIL", ErrorCode.DUPLICATE_EMAIL);

        private final String columnHint;
        private final ErrorCode errorCode;

        UniqueViolation(String columnHint, ErrorCode errorCode) {
            this.columnHint = columnHint;
            this.errorCode = errorCode;
        }

        static RuntimeException translate(DataIntegrityViolationException cause) {
            String message = headerOf(cause).toUpperCase();
            return Arrays.stream(values())
                    .filter(violation -> message.contains(violation.columnHint))
                    .findFirst()
                    .<RuntimeException>map(violation -> new BusinessException(violation.errorCode))
                    .orElse(cause);
        }

        private static String headerOf(DataIntegrityViolationException cause) {
            // H2/PostgreSQL 모두 "; SQL statement" 또는 "; SQL" 이후에 INSERT 문 본문이 붙는다.
            // 그 부분을 잘라내야 INSERT 컬럼 리스트의 오매칭을 막을 수 있다.
            String full = String.valueOf(cause.getMostSpecificCause());
            int sqlPos = full.indexOf("; SQL");
            return sqlPos > 0 ? full.substring(0, sqlPos) : full;
        }
    }
}
