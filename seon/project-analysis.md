# 프로젝트 분석 보고서

> 분석 일자: 2026-04-17  
> 프로젝트: 배달 주문 관리 플랫폼  
> 기술 스택: Java 17 · Spring Boot 3.3.4 · JPA · PostgreSQL · Gradle

---

## 1. 프로젝트 개요

**아키텍처**: Monolithic Layered (Controller → Service → Repository)  
**현재 상태**: 초기 구축 단계 — User 도메인 일부만 구현, 전체 완성도 약 **5~10%**

---

## 2. 디렉토리 구조

```
Sparta/
├── CLAUDE.md
├── README.md
├── build.gradle
├── settings.gradle
├── docker-compose.yml
├── Dockerfile
├── docs/
│   ├── 00-OVERVIEW.md
│   ├── 01-functional-spec.md
│   ├── 02-domain-spec.md
│   ├── 03-data-spec.md
│   ├── 04-api-spec.md
│   ├── 05-service-spec.md
│   ├── 06-infra-spec.md
│   ├── 07-code-design.md
│   └── git-convention.md
└── src/main/java/com/example/delivery/
    ├── DeliveryApplication.java
    ├── global/
    │   ├── common/exception/          # ErrorCode, BusinessException, ResourceNotFoundException
    │   └── infrastructure/presentation/advice/  # GlobalExceptionHandler
    └── user/                          # 유일한 구현 도메인
        ├── application/service/UserServiceV1.java
        ├── domain/entity/UserEntity.java
        ├── domain/entity/UserRole.java
        ├── domain/repository/UserRepository.java
        ├── infrastructure/repository/FakeUserRepository.java
        └── presentation/controller/UserControllerV1.java
```

---

## 3. 치명적 버그 (즉시 수정 필요)

### BUG-01: `UserEntity`에 JPA 어노테이션 없음

**파일**: `src/main/java/com/example/delivery/user/domain/entity/UserEntity.java`

현재 코드에 `@Entity`, `@Table`, `@Id`, `@Column`이 전혀 없음.  
→ **현재 상태로는 DB 매핑 자체가 불가능하여 실행 시 정상 동작하지 않음.**

```java
// 현재 (잘못됨)
@Getter
@Builder
public class UserEntity {
    private String username;
    ...
}

// 수정 후
@Entity
@Table(name = "p_user")
@Getter
@Builder
public class UserEntity {
    @Id
    @Column(length = 10)
    private String username;

    @Column(length = 100, nullable = false)
    private String nickname;

    @Column(length = 255, nullable = false, unique = true)
    private String email;

    @Column(length = 255, nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private UserRole role;

    @Column(name = "is_public")
    private Boolean isPublic = true;
}
```

### BUG-02: 실제 JPA Repository 없음

**파일**: `src/main/java/com/example/delivery/user/infrastructure/repository/FakeUserRepository.java`

`ConcurrentHashMap` 기반 메모리 저장소만 존재. 운영 DB와 연동되는 `JpaRepository` 구현체가 없음.

```java
// 추가 필요
@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

### BUG-03: BaseEntity 없음

설계 문서에 명시된 Audit 컬럼 (`created_at`, `created_by`, `updated_at`, `updated_by`, `deleted_at`, `deleted_by`) 자동화가 전혀 구현되지 않음.

```java
// 추가 필요
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(updatable = false, length = 100)
    private String createdBy;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(length = 100)
    private String updatedBy;

    private LocalDateTime deletedAt;

    @Column(length = 100)
    private String deletedBy;
}
```

---

## 4. 주요 미구현 항목

### 4.1 도메인별 구현 현황

| 도메인 | 구현 여부 | 우선순위 |
|--------|----------|--------|
| AUTH (회원가입/로그인/JWT) | 미구현 | P0 |
| USER (CRUD 완성) | 40% | P0 |
| AREA | 미구현 | P0 |
| CATEGORY | 미구현 | P0 |
| STORE | 미구현 | P0 |
| MENU | 미구현 | P0 |
| ORDER | 미구현 | P0 |
| PAYMENT | 미구현 | P0 |
| ADDRESS | 미구현 | P0 |
| REVIEW | 미구현 | P1 |
| AI (Gemini 연동) | 미구현 | P1 |

### 4.2 공통 인프라 미구현 항목

| 항목 | 설명 |
|------|------|
| Spring Security + JWT | 현재 모든 API 공개 상태 |
| BaseEntity + @EnableJpaAuditing | Audit 자동화 불가 |
| QueryDSL 설정 | 복합 검색 구현 불가 |
| 프로필별 설정 | local/test/prod 분리 없음 |
| 테스트 코드 | 전무 |
| Swagger(springdoc-openapi) | API 문서화 없음 |

---

## 5. 의존성 누락 (`build.gradle`에 추가 필요)

```gradle
// 인증
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.3'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.3'

// QueryDSL
implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
annotationProcessor 'com.querydsl:querydsl-apt:5.0.0:jakarta'

// API 문서화
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.0.4'

// 테스트 (Testcontainers)
testImplementation 'org.testcontainers:testcontainers:1.19.3'
testImplementation 'org.testcontainers:postgresql:1.19.3'
```

---

## 6. 보안 문제

| 심각도 | 항목 | 현황 | 권장 조치 |
|--------|------|------|----------|
| Critical | JWT 인증 없음 | 모든 API 무인증 공개 | Spring Security + jjwt 추가 |
| Critical | DB 비밀번호 하드코드 | `docker-compose.yml`에 평문 노출 | `.env` 파일로 분리 |
| High | Soft Delete 미구현 | 데이터 영구 삭제 위험 | BaseEntity에 `deleted_at` 추가 + `@SQLRestriction` |
| High | Audit 자동화 없음 | 수동 관리 필요 | `@EnableJpaAuditing` 설정 |

---

## 7. 성능 문제

1. **N+1 쿼리 위험**: Store 조회 시 Review 평균 평점을 매번 재조회할 수 있음
   - 설계 문서의 `average_rating` 캐싱 컬럼 반드시 구현 필요
2. **인덱스 미반영**: `03-data-spec.md`의 인덱스 가이드가 코드에 반영되지 않음
3. **페이지네이션 미구현**: 목록 API에 `Pageable` 없음

---

## 8. 종합 평가

| 항목 | 점수 | 비고 |
|------|------|------|
| 설계 문서 | 9/10 | 상세하고 명확함 |
| 코드 구조 | 6/10 | 도메인 분리 좋으나 대부분 미완성 |
| JPA/DB 연동 | 1/10 | @Entity 어노테이션 누락으로 실질적 동작 불가 |
| 보안 | 3/10 | JWT/인증 없음, 민감정보 노출 |
| 테스트 | 0/10 | 전무 |
| 배포 준비 | 4/10 | 기본 구조만 있음 |
| **종합** | **4/10** | |

---

## 9. 권장 구현 순서

### Phase 1 — 기본 인프라 (최우선)
1. `BaseEntity` 구현 + `@EnableJpaAuditing` 설정
2. `UserEntity`에 JPA 어노테이션 추가
3. 실제 `UserJpaRepository` 구현 (FakeRepository 교체)
4. Spring Security + JWT 설정
5. Auth 도메인 구현 (회원가입/로그인)

### Phase 2 — 핵심 도메인
- Area, Category, Store, Menu 구현
- 권한 검증 미들웨어
- N+1 방지 (`average_rating` 캐싱)

### Phase 3 — 주문/결제
- Order, OrderItem, Payment, Review 구현
- 주문 상태 흐름 검증
- 5분 취소 제한 로직

### Phase 4 — AI + 완성
- Gemini 클라이언트 구현
- `AiRequestLog` 저장
- 전체 테스트 코드 작성 (Repository/Service/Controller)

### Phase 5 — 배포 준비
- 프로필별 설정 (`local`, `test`, `prod`)
- Logback 로깅 설정
- Docker 최종 테스트