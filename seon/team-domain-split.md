# 6인 도메인 분배 계획

> 작성일: 2026-04-17

---

## 구현해야 할 전체 도메인 현황

### 공통 인프라 (모든 도메인의 전제 조건)

| 항목 | 설명 | 현황 |
|------|------|------|
| `BaseEntity` | Audit 6개 컬럼 자동화 | 미구현 |
| `SecurityConfig` + `JwtTokenProvider` + `JwtAuthenticationFilter` | 인증/인가 전체 | 미구현 |
| `application.yml` 프로필 분리 (local/test/prod) | 환경 분리 | 미구현 |
| `ErrorCode` 확장 | 도메인별 에러 코드 추가 | 일부만 존재 |

### 도메인 의존 관계

```
[BaseEntity / Security / JWT]   ← 전원 블로킹, 최우선
         ↓
    [Auth]  [User]              ← JWT 필터가 User를 조회
         ↓
  [Area]  [Category]            ← 독립, 단순 CRUD
         ↓
       [Store]                  ← Area + Category + User(OWNER) 의존
         ↓
    [Menu]  [AI]                ← Store 의존 / AI는 Menu와 묶임
         ↓
  [Address] → [Order] → [Payment]
                ↓
            [Review]
```

---

## 6명 분배안

### Dev 1 — 공통 인프라 + Auth
> 전원의 블로킹 포인트. 가장 먼저 완성해야 나머지가 JWT 테스트 가능.

| 작업 | 파일 |
|------|------|
| `BaseEntity` 구현 | `global/infrastructure/entity/BaseEntity.java` |
| `SecurityConfig`, `JwtTokenProvider`, `JwtAuthenticationFilter` | `global/infrastructure/config/security/` |
| `AuditorAware` 설정 | SecurityContext에서 username 추출 |
| `application.yml` 프로필 분리 | local(H2) / test(H2) / prod(PostgreSQL) |
| `Auth` 도메인 전체 | 회원가입, 로그인 → JWT 발급 |
| `ErrorCode` 기반 정비 | 모든 도메인 에러 코드 초안 작성 |

**예상 난이도**: 높음 (JWT 필터가 User 조회에 의존하므로 User Entity와 협업 필요)

---

### Dev 2 — User + Address
> User는 이미 40% 구현되어 있고, Address는 User에만 의존해서 비교적 독립적.

| 작업 | 파일 |
|------|------|
| `UserEntity` JPA 어노테이션 추가, `FakeRepository` → `JpaRepository` 교체 | `user/` |
| User CRUD 완성 (목록/상세/수정/삭제/권한변경) | `UserServiceV1`, `UserControllerV1` |
| `Address` 도메인 전체 (등록/목록/수정/삭제/기본배송지 설정) | `address/` |

**예상 난이도**: 중간

---

### Dev 3 — Area + Category + Store
> Area/Category는 단순 CRUD, Store는 이 둘에 의존. 한 사람이 순차적으로 처리하기 좋음.

| 작업 | 파일 |
|------|------|
| `Area` CRUD (MANAGER/MASTER 권한) | `area/` |
| `Category` CRUD (MANAGER/MASTER 권한) | `category/` |
| `Store` CRUD + 숨김 처리 + `average_rating` 컬럼 정의 | `store/` |
| [도전] QueryDSL 복합 검색 (keyword + categoryId + areaId) | `StoreRepositoryCustomImpl` |

**예상 난이도**: 중간~높음 (QueryDSL 포함 시)

---

### Dev 4 — Menu + AI
> Menu는 Store에 의존. AI는 Menu 등록 시 옵션으로 호출되므로 자연스럽게 묶임.

| 작업 | 파일 |
|------|------|
| `Menu` CRUD + 숨김 처리 | `menu/` |
| `GeminiClient` 구현 (HTTP 호출) | `ai/infrastructure/GeminiClient.java` |
| `AiServiceV1` — prompt 검증, 자동 suffix 부착, 로그 저장 | `ai/application/service/` |
| `AiRequestLogEntity` — BaseEntity 미상속, `created_at/by`만 직접 보유 | `ai/domain/entity/` |
| `POST /api/v1/ai/product-description` 엔드포인트 | `ai/presentation/controller/` |

**예상 난이도**: 중간 (Gemini API 연동이 미지의 영역)

---

### Dev 5 — Order + OrderItem
> 가장 복잡한 도메인. 상태 흐름, 5분 취소 제한, 단가 스냅샷 등 비즈니스 로직이 많음.

| 작업 | 파일 |
|------|------|
| `OrderEntity` — `OrderStatus` 열거형, `changeStatusByOwner()`, `cancelByCustomer(Clock)` 도메인 메서드 | `order/domain/entity/` |
| `OrderItemEntity` — 단가 스냅샷 (`unit_price`) | `order/domain/entity/` |
| `OrderServiceV1` — 생성, 조회, 상태변경, 취소(5분), 요청사항 수정 | `order/application/service/` |
| `OrderControllerV1` | `order/presentation/controller/` |

**예상 난이도**: 높음

---

### Dev 6 — Payment + Review
> 둘 다 Order에 의존하므로 Order 완성 후 진행. 규모가 작아 한 사람이 처리 가능.

| 작업 | 파일 |
|------|------|
| `Payment` 전체 (결제 생성, 조회, 상태수정, 삭제) | `payment/` |
| `Review` 전체 (작성/조회/수정/삭제 + `average_rating` 재집계) | `review/` |
| 리뷰 작성 시 `Store.averageRating` 재집계 — 동일 트랜잭션 | `ReviewServiceV1` ↔ `StoreService` |

**예상 난이도**: 중간 (트랜잭션 경계 설계가 핵심)

---

## 작업 순서 타임라인

```
Week 1
  Dev1: 인프라/Auth ━━━━━━━━━━━━━━━┓
  Dev2: UserEntity 수정 → User CRUD ┃ (JWT 필터 완성 전까지 임시 bypass)
  Dev3: Area → Category ━━━━━━━━━━━┛

Week 2
  Dev1 완료 → 전원 JWT 통합
  Dev3: Store
  Dev4: Menu (Store 완성 후)
  Dev2: Address

Week 3
  Dev5: Order (Store + Menu + Address 완성 후)
  Dev4: AI 연동 마무리

Week 4
  Dev6: Payment (Order 완성 후)
  Dev6: Review (Order + Store 완성 후)
  전원: 테스트 코드 + 통합 테스트
```

---

## 핵심 협업 포인트

1. **Dev 1이 가장 먼저 `BaseEntity`와 JWT 필터 골격을 PR** → 나머지 5명이 Entity 작성 시작 가능
2. **Dev 2 ↔ Dev 1 긴밀 협업**: `JwtAuthenticationFilter`가 `UserRepository.findByUsername`을 호출하므로 User Entity/Repository는 Dev 1 작업 전에 최소한 골격이 있어야 함
3. **Dev 6은 Order가 블로킹**: Dev 5가 Order를 완성할 때까지 Payment/Review 엔티티 스켈레톤만 미리 준비
4. **인터페이스(도메인 Repository) 먼저 합의**: 다른 도메인 Service에서 FK 조회 시 어떤 메서드를 쓸지 사전에 팀 내 약속 필요