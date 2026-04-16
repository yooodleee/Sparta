# 05. 서비스 로직 명세 (간단 버전)

> 이전: [API 명세](04-api-spec.md) · 다음: [인프라 명세](06-infra-spec.md)

주요 유스케이스의 핵심 흐름만 요약한다. (상세 시퀀스 다이어그램은 구현 단계에서 필요 시 추가)

---

## 1. 회원가입 / 로그인

### 1.1 `AuthService.signup(ReqSignup)`

1. username/password/email 형식 Validation
2. `UserRepository.existsByUsername / existsByEmail` 중복 확인 → 409
3. `BCryptPasswordEncoder.encode(password)`
4. `UserEntity` 저장 → 응답 DTO 변환

### 1.2 `AuthService.login(ReqLogin)`

1. `UserRepository.findByUsername` → 없으면 401
2. `passwordEncoder.matches` → 실패 시 401
3. `JwtTokenProvider.generate(username, role)` → JWT
4. 응답: `{ accessToken, username, role }`

### 1.3 권한 재검증 필터

- `JwtAuthenticationFilter`에서 JWT 파싱
- `UserRepository.findByUsername`으로 현재 role 조회
- payload.role ≠ DB.role 이거나 `deleted_at != null`이면 401/403
- SecurityContext에 `UsernamePasswordAuthenticationToken` 저장

## 2. 가게 / 메뉴

### 2.1 `StoreService.create(ReqCreate, loginUser)`

- loginUser.role == OWNER 확인
- 카테고리/지역 존재 여부 체크
- `owner_id = loginUser.username`으로 저장

### 2.2 `StoreService.searchStores(keyword, categoryId, areaId, pageable)` [도전: QueryDSL]

- BooleanBuilder 조합:
    - keyword → `name LIKE %...%`
    - categoryId → `category_id = ?`
    - areaId → `area_id = ?`
    - `deleted_at IS NULL AND is_hidden = false`
- Page 반환 (평균 평점은 엔티티 필드 그대로)

### 2.3 `MenuService.create(ReqCreate, loginUser)`

1. 가게 소유자 검증 (`store.owner_id == loginUser.username` 또는 MANAGER/MASTER)
2. `aiDescription == true`:
    - `aiPrompt` 필수/100자 체크
    - `AiClient.generateDescription(aiPrompt)` 호출
    - 응답을 `description`에 주입
    - `AiRequestLog` 저장
3. `MenuEntity` 저장

## 3. 주문 (핵심)

### 3.1 `OrderService.create(ReqOrder, loginUser)` — `@Transactional`

1. 가게/배송지/메뉴 존재·소속 검증 (모든 메뉴가 동일 store, 숨김 아님)
2. `totalPrice = Σ(quantity × menu.price)` 계산
3. `OrderEntity(status=PENDING, totalPrice)` 저장
4. 각 아이템 → `OrderItemEntity(unit_price=menu.price)` 저장 (Cascade)
5. 응답: 생성된 주문 정보

### 3.2 `OrderService.cancel(orderId, loginUser)` — `@Transactional`

1. 주문 조회 + 소유자 검증
2. `now() - order.createdAt <= 5분` 체크
    - 초과 시 `ORDER_CANCEL_TIMEOUT` 400
3. 상태 `CANCELLED` 로 (도메인 요구사항상 별도 명시 없으나 취소는 Soft 처리 + status 변경)
    - MASTER는 5분 초과도 가능

### 3.3 `OrderService.changeStatus(orderId, nextStatus, loginUser)`

- 역할별 정책:
    - OWNER: 본인 가게 + 정의된 순서만 (`PENDING → ACCEPTED → COOKING → DELIVERING → DELIVERED → COMPLETED`)
    - MANAGER/MASTER: 자유 전이
- 역방향/점프 시 `INVALID_ORDER_STATUS` 400

### 3.4 `OrderService.updateRequest(orderId, request, loginUser)`

- CUSTOMER(본인) + `status == PENDING`일 때만 `request` 수정
- MASTER는 제한 없음

## 4. 결제

### 4.1 `PaymentService.pay(orderId, ReqPay, loginUser)` — `@Transactional`

1. 주문 존재 + 본인 주문 확인
2. 이미 결제 존재 시 409
3. `paymentMethod == CARD` 검증
4. `PaymentEntity(amount=order.totalPrice, status=COMPLETED)` 저장
    - PG 미연동이므로 즉시 COMPLETED로 기록

## 5. 리뷰

### 5.1 `ReviewService.create(orderId, ReqReview, loginUser)` — `@Transactional`

1. 주문 조회, 본인 주문 확인
2. `order.status == COMPLETED` 확인
3. `reviewRepository.existsByOrderId(orderId)` → 중복 시 409
4. `ReviewEntity` 저장 (`store_id`는 주문 store_id로 역정규화)
5. `store.recalculateAverageRating()` 호출 → `AVG(rating)` 재계산 후 업데이트
    - 같은 트랜잭션 내 수행, N+1 방지

### 5.2 리뷰 수정/삭제

- 같은 트랜잭션 내 평균 평점 재계산 (위와 동일)

## 6. AI 연동

### 6.1 `AiService.generateProductDescription(prompt, loginUser)`

1. prompt 길이 검증 (100자)
2. `finalPrompt = prompt + "답변을 최대한 간결하게 50자 이하로"`
3. `GeminiClient.call(finalPrompt)` — RestTemplate/WebClient 동기 호출
4. 응답/실패 모두 `AiRequestLog` 저장 (request_type=PRODUCT_DESCRIPTION)
5. 응답 반환

### 6.2 메뉴 등록과 조합

- `MenuService.create` 내부에서 `AiService.generateProductDescription` 호출 후 설명 주입
- 같은 트랜잭션에서 로그 저장 (AI 호출 실패 시 롤백 정책: 메뉴 저장 실패 처리 or 로그만 실패 기록 — 초기엔 예외 전파 방식)

## 7. 배송지

### 7.1 `AddressService.setDefault(addressId, loginUser)` — `@Transactional`

1. 본인 배송지 확인
2. 기존 default 레코드를 `is_default=false`로 UPDATE
3. 대상 레코드를 `is_default=true`로 UPDATE

## 8. 공통 처리

### 8.1 글로벌 예외 (`GlobalExceptionHandler`)

- `MethodArgumentNotValidException` → 400 `VALIDATION_ERROR`
- `BusinessException` 계층 → 상태 코드 매핑
- 그 외 → 500 `INTERNAL_ERROR`

### 8.2 Audit 자동 채움

- `@EntityListeners(AuditingEntityListener.class)` + `@EnableJpaAuditing`
- `AuditorAware<String>` → SecurityContext의 username 반환

### 8.3 Soft Delete

- `deleted_at = now(), deleted_by = ?` 업데이트
- 조회 시 기본 `WHERE deleted_at IS NULL` 조건 추가 (JPA `@SQLRestriction` 또는 Repository 메서드)

## 9. 트랜잭션 경계 요약

| 유스케이스         | 경계                                           | 이유                  |
|---------------|----------------------------------------------|---------------------|
| 회원가입          | 단일 `save`                                    |                     |
| 주문 생성         | Order + OrderItem[*]                         | 원자성                 |
| 주문 상태 변경 / 취소 | 단일 update                                    |                     |
| 결제            | Payment 저장 (Order status 업데이트 X — 주문 상태와 독립) |                     |
| 리뷰 CUD        | Review + Store.avg_rating                    | 평점 N+1 방지           |
| 메뉴 + AI       | Menu + AiRequestLog                          | AI 실패 시 롤백 정책 확정 필요 |
