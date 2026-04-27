# 07. 코드 레벨 설계 (아주 간단 버전)

> 이전: [인프라 명세](06-infra-spec.md) · 다음: [애플리케이션 흐름](08-app-flow.md)

도메인 패키지 단위로 Controller / Service / Repository / Entity / DTO를 배치한다. 요구사항의 패키지 구조를 기준으로 핵심 클래스 시그니처만 간략히 정의한다.

---

## 1. 패키지 구조

```
com.example.delivery
├── global
│   └── infrastructure
│       ├── config
│       │   ├── security
│       │   │   ├── SecurityConfig
│       │   │   ├── JwtTokenProvider
│       │   │   └── JwtAuthenticationFilter
│       │   └── QueryDslConfig            ← [도전]
│       ├── presentation
│       │   └── advice/GlobalExceptionHandler
│       └── entity/BaseEntity             ← Audit 필드
├── auth         (Auth)
├── user         (User)
├── area         (Area)
├── category     (Category)
├── store        (Store)
├── menu         (Menu)
├── order        (Order + OrderItem)
├── payment      (Payment)
├── review       (Review)
├── address      (Address)
├── ai           (AI + AiRequestLog)
└── DeliveryApplication
```

도메인 패키지 내부는 공통적으로:

```
{domain}/
 ├── application/service/{Domain}ServiceV1
 ├── domain/
 │   ├── entity/{Domain}Entity
 │   └── repository/{Domain}Repository
 ├── infrastructure/repository/...Custom  (필요 시 QueryDSL)
 └── presentation/
     ├── controller/{Domain}ControllerV1
     └── dto/{request,response}/...
```

## 2. 공통 기반 (global)

### `BaseEntity` (MappedSuperclass)

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
  @CreatedDate     LocalDateTime createdAt;
  @CreatedBy       String        createdBy;
  @LastModifiedDate LocalDateTime updatedAt;
  @LastModifiedBy  String        updatedBy;
  LocalDateTime deletedAt;
  String        deletedBy;
  public void softDelete(String by){ this.deletedAt = now(); this.deletedBy = by; }
}
```

> `p_order_item`, `p_ai_request_log`는 상속 대신 `created_at/by`만 직접 보유.

### `SecurityConfig`

- `SecurityFilterChain` Bean
- `BCryptPasswordEncoder`
- JWT 필터 등록, CSRF disable, Stateless

### `JwtTokenProvider`

```java
String  generate(String username, Role role);
Claims  parse(String token);
boolean isValid(String token);
```

### `JwtAuthenticationFilter`

- Bearer 토큰 추출 → `JwtTokenProvider.parse`
- `UserRepository.findByUsername`으로 role 재검증
- 통과 시 `SecurityContext`에 Authentication 저장, 실패 시 401/403

### `GlobalExceptionHandler`

- `@RestControllerAdvice`
- Validation / BusinessException / 기타 예외 매핑

## 3. 도메인별 주요 클래스

### 3.1 User

- `UserEntity` (PK `username`, role `UserRole`)
- `UserRepository extends JpaRepository<UserEntity, String>`
    - `existsByUsername`, `existsByEmail`, `findByUsername`
- `UserServiceV1`: `find`, `update`, `delete`, `changeRole`
- `UserControllerV1`: GET/PATCH/DELETE `/users/**`

### 3.2 Auth

- `AuthServiceV1`
    - `signup(ReqSignupDto)` → `ResUserDto`
    - `login(ReqLoginDto)` → `ResLoginDto { accessToken, username, role }`
- `AuthControllerV1`: POST `/auth/signup`, `/auth/login`

### 3.3 Area / Category

- `AreaEntity(areaId UUID, name, city, district, isActive)`
- `CategoryEntity(categoryId UUID, name)`
- `AreaServiceV1`, `CategoryServiceV1`: 표준 CRUD
- `AreaControllerV1`, `CategoryControllerV1`

### 3.4 Store

- `StoreEntity(storeId UUID, owner(UserEntity), category, area, name, address, phone, averageRating, isHidden)`
    - `recalculateAverageRating(List<Integer> ratings)` 메서드
- `StoreRepository extends JpaRepository + StoreRepositoryCustom`
- `StoreRepositoryCustomImpl` (QueryDSL) [도전]
    - `Page<StoreEntity> search(keyword, categoryId, areaId, pageable)`
- `StoreServiceV1`: `create`, `search`, `get`, `update`, `delete`, `toggleHide`
- `StoreControllerV1`: `/stores/**`

### 3.5 Menu

- `MenuEntity(menuId UUID, store, name, price, description, isHidden)`
- `MenuRepository`
- `MenuServiceV1`
    - `create(storeId, ReqCreateMenuDto, loginUser)` — AI 옵션 시 `AiServiceV1.generateDescription` 호출
    - `update/delete/restore/toggleHide/get/list`
- `MenuControllerV1`

### 3.6 Order + OrderItem

-
`OrderEntity(orderId UUID, customer, store, address, orderType, status(OrderStatus), totalPrice, request, items: List<OrderItemEntity>)`
    - `OrderStatus { PENDING, ACCEPTED, COOKING, DELIVERING, DELIVERED, COMPLETED, CANCELLED }`
    - `changeStatusByOwner(OrderStatus next)` — 순서 검증
    - `cancelByCustomer(Clock clock)` — 5분 체크 + 상태 전이
    - `updateRequest(String newRequest)` — PENDING만
- `OrderItemEntity(orderItemId UUID, order, menu, quantity, unitPrice)`
- `OrderRepository`, `OrderItemRepository`
- `OrderServiceV1`
    - `create(ReqCreateOrderDto, loginUser)`
    - `get/list(filter, loginUser)`
    - `changeStatus(orderId, next, loginUser)`
    - `cancel(orderId, loginUser)`
    - `updateRequest(orderId, request, loginUser)`
- `OrderControllerV1`: `/orders/**`

### 3.7 Payment

- `PaymentEntity(paymentId UUID, order, paymentMethod, status(PaymentStatus), amount)`
    - `PaymentStatus { PENDING, COMPLETED, CANCELLED }`
- `PaymentRepository`
- `PaymentServiceV1`: `pay`, `get`, `list`, `updateStatus`, `delete`
- `PaymentControllerV1`

### 3.8 Review

- `ReviewEntity(reviewId UUID, order, store, customer, rating, content)`
- `ReviewRepository`: `existsByOrderId`, `findAllByStoreId`
- `ReviewServiceV1`
    - `create/update/delete` 내부에서 `Store.recalculateAverageRating` 호출
- `ReviewControllerV1`

### 3.9 Address

- `AddressEntity(addressId UUID, user, alias, address, detail, zipCode, isDefault)`
- `AddressRepository`: `findAllByUserId`, `findByUserIdAndIsDefaultTrue`
- `AddressServiceV1`: `create/list/get/update/delete/setDefault`
- `AddressControllerV1`

### 3.10 AI

- `AiRequestLogEntity(aiLogId UUID, user, requestText, responseText, requestType, createdAt, createdBy)` (BaseEntity
  미상속)
- `AiRequestLogRepository`
- `GeminiClient` (infrastructure)
    - `String generate(String prompt)` — Gemini API 호출, 타임아웃/에러 전파
- `AiServiceV1`
    - `generateProductDescription(prompt, loginUser)`:
        1. prompt 100자 체크
        2. `"답변을 최대한 간결하게 50자 이하로"` 부착
        3. GeminiClient 호출
        4. AiRequestLog 저장
        5. 결과 반환
- `AiControllerV1`: POST `/ai/product-description`

## 4. DTO 네이밍 규칙

- Request: `Req{Action}{Domain}Dto` (예: `ReqSignupDto`, `ReqCreateStoreDto`)
- Response: `Res{Domain}Dto` (예: `ResUserDto`, `ResStoreDto`)
- 리스트 응답은 Page 구조로 래핑 (공통 `PageResponse<T>` 추천)

## 5. 공통 응답 래퍼 (제안)

```java
public record ApiResponse<T>(int status, String message, T data) {
  public static <T> ApiResponse<T> ok(T data){ return new ApiResponse<>(200, "SUCCESS", data); }
  public static <T> ApiResponse<T> created(T data){ return new ApiResponse<>(201, "CREATED", data); }
}
```

## 6. 테스트 가이드 (요약)

- Repository: 미작성 (팀 컨벤션), 개인 판단으로 필요 시 작성
- Service: Mockito mock 또는 인터페이스 기반 Fake 구현 — 성공/실패 케이스 필수
- Controller: `@WebMvcTest` + MockMvc — 권한/Validation 검증
- [도전] Testcontainers + PostgreSQL로 통합 테스트 확장 가능

## 7. 주요 설계 포인트 체크

- [x] 도메인 패키지 독립성 유지
- [x] Entity는 상태 변경 메서드를 도메인 내부에 캡슐화 (예: Order.cancelByCustomer)
- [x] Service V1 네이밍으로 버전 확장 여지 확보
- [x] Controller는 DTO 변환 + 서비스 호출에만 집중
- [x] Security/JWT는 global 한 곳에 집중, 도메인 오염 없음
- [x] AI 클라이언트는 `ai/infrastructure`로 격리하여 테스트 모킹 용이
