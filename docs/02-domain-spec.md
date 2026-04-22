# 02. 도메인 명세

> 이전: [기능 명세](01-functional-spec.md) · 다음: [데이터 명세](03-data-spec.md)

---

## 1. 도메인 목록

| 도메인          | 설명        | PK                        |
|--------------|-----------|---------------------------|
| User         | 사용자/계정    | UUID (`username`은 UNIQUE) |
| Area         | 운영 지역     | UUID                      |
| Category     | 음식점 분류    | UUID                      |
| Store        | 가게        | UUID                      |
| Menu         | 메뉴/상품     | UUID                      |
| Order        | 주문 (루트)   | UUID                      |
| OrderItem    | 주문 상품(라인) | UUID                      |
| Review       | 리뷰/평점     | UUID                      |
| Payment      | 결제 내역     | UUID                      |
| Address      | 배송지       | UUID                      |
| AiRequestLog | AI 호출 로그  | UUID                      |

## 2. 애그리거트 경계

- **Order 애그리거트**: `Order` ─< `OrderItem` (부모-자식, Cascade ALL, OrphanRemoval)
- 나머지는 독립 루트. FK만 가짐 (`Review.order_id`, `Payment.order_id` 등).
- `Review.store_id`는 가게별 리뷰 검색 성능을 위해 역정규화로 보유.

## 3. 상태 / 열거 값

### 3.1 User.role

`CUSTOMER`, `OWNER`, `MANAGER`, `MASTER`

### 3.2 Order.status — 상태 흐름

```
PENDING → ACCEPTED → COOKING → DELIVERING → DELIVERED → COMPLETED
                                                               ↑
                                                    (CUSTOMER 취소: PENDING 상태 + 5분 이내만 가능)
```

- `CUSTOMER`: 생성(`PENDING`) + 5분 이내 취소만 가능
- `OWNER`: 위 순서대로만 전이 가능 (역방향 불가)
- `MANAGER`, `MASTER`: 모든 상태 전이 가능

### 3.3 Order.order_type

- `ONLINE` (현재 유일한 값)

### 3.4 Payment.status

- `PENDING` → `COMPLETED` / `CANCELLED`

### 3.5 Payment.method

- `CARD` (유일한 허용 값)

### 3.6 AiRequestLog.request_type

- `PRODUCT_DESCRIPTION`

## 4. 권한 체계 (도메인 불변식)

| 기능            | CUSTOMER            | OWNER    | MANAGER | MASTER |
|---------------|---------------------|----------|---------|--------|
| 가게 등록         | ❌                   | ✅        | ❌       | ✅      |
| 가게 수정/삭제      | ❌                   | ✅(본인)    | ✅       | ✅      |
| 메뉴 CRUD       | ❌                   | ✅(본인 가게) | ✅       | ✅      |
| 주문 생성         | ✅                   | ❌        | ❌       | ❌      |
| 주문 상태 변경      | ❌                   | ✅(본인 가게) | ✅       | ✅      |
| 주문 취소(5분 내)   | ✅(본인)               | ❌        | ❌       | ✅      |
| 리뷰 작성         | ✅(본인 주문, COMPLETED) | ❌        | ❌       | ❌      |
| 사용자 관리(대상 CUSTOMER·OWNER) | ❌    | ❌        | ✅       | ✅      |
| 사용자 관리(대상 MANAGER·MASTER) | ❌    | ❌        | ❌(본인 제외) | ✅(본인 role 변경 제외) |
| MANAGER 생성/삭제 | ❌                   | ❌        | ❌       | ✅      |
| 카테고리/지역 관리    | ❌                   | ❌        | ✅       | ✅      |

## 5. 핵심 불변식(Invariants)

### User

- `username`: `^[a-z0-9]{4,10}$`
- `password`: 8~15자, 대소문자 + 숫자 + 특수문자 각 1자 이상 포함
- `email`: UNIQUE

### Store

- `owner_id`는 반드시 `OWNER` role
- `(category_id, area_id)` NOT NULL
- `average_rating`: 0.0 ~ 5.0, 리뷰 CUD 시 트랜잭션 내 재계산

### Menu

- `price` > 0
- `is_hidden`과 `deleted_at`은 별개로 동작 (숨김 ≠ 삭제)
- `aiDescription=true` 시 `aiPrompt` 필수, 최대 100자

### Order

- 최소 1개 이상의 `OrderItem` 보유 (`items.size() >= 1`)
- `total_price = Σ(item.quantity × item.unit_price)` (생성 시 1회 계산 후 스냅샷)
- `created_at` 기준 5분 이내에만 CUSTOMER 취소 가능
- 상태 전이는 역방향 불가 (OWNER 기준)

### OrderItem

- `quantity > 0`
- `unit_price`는 주문 시점 메뉴 가격 스냅샷 (가게 가격 변경과 무관)

### Review

- `(order_id)` UNIQUE → 1주문 1리뷰
- `rating ∈ [1,5]` (정수)
- 작성 가능 조건: `Order.status = COMPLETED` AND 작성자 = `order.customer`

### Payment

- `(order_id)` UNIQUE → 1주문 1결제
- `amount = order.total_price`
- `method = CARD`

### Address

- 한 유저당 `is_default=true`는 최대 1개
- `address` 필수 (NOT NULL)

### AiRequestLog

- `request_text` 100자 이내
- 서버가 prompt 끝에 `"답변을 최대한 간결하게 50자 이하로"` 자동 부착
- 수정/삭제 불가 (insert-only)
- 'is_applied=true'일 경우, 반드시 'menu_id'가 존재해야 함

## 6. 도메인 관계 요약

| 관계                     | 카디널리티 | 비고                      |
|------------------------|-------|-------------------------|
| User(OWNER) ↔ Store    | 1:N   |                         |
| User(CUSTOMER) ↔ Order | 1:N   |                         |
| User ↔ Address         | 1:N   |                         |
| User ↔ AiRequestLog    | 1:N   | 요청 주체 (누가 AI를 호출했는지)    |
| Area ↔ Store           | 1:N   |                         |
| Category ↔ Store       | 1:N   |                         |
| Store ↔ Menu           | 1:N   |                         |
| Store ↔ Order          | 1:N   |                         |
| Store ↔ Review         | 1:N   | 역정규화                    |
| Order ↔ OrderItem      | 1:N   | 애그리거트 내부                |
| Order ↔ Review         | 1:1   | UNIQUE                  |
| Order ↔ Payment        | 1:1   | UNIQUE                  |
| Menu ↔ OrderItem       | 1:N   |                         |
| Address ↔ Order        | 1:N   |                         |
| Menu ↔ AiRequestLog    | 1:N   | 요청 대상(어떤 메뉴의 설명을 생성했는지) |