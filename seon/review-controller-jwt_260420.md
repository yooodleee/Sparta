# Review Controller - JWT 반영 및 전체 구현

작업 브랜치: `feature/1-implement-review-logic`
작업일: 2026-04-20

---

## 변경 사항 요약

### 1. JWT 반영 - `createReview` 수정

#### `ReqCreateReviewDto` 필드 제거
```java
// 제거됨 - JWT 토큰에서 추출하므로 요청 바디 불필요
@NotBlank(message = "작성자 ID는 필수 입력 항목입니다.")
String customerId
```

#### `ReviewService.createReview` 시그니처 변경
```java
// Before
public ResReviewDto createReview(UUID orderId, ReqCreateReviewDto request)

// After
public ResReviewDto createReview(UUID orderId, ReqCreateReviewDto request, UserPrincipal principal)
```
- `customerId`를 요청 바디가 아닌 `principal.username()`으로 추출

#### `ReviewController.createReview` 파라미터 추가
```java
@PostMapping("/orders/{orderId}/reviews")
@ResponseStatus(HttpStatus.CREATED)
public ApiResponse<ResReviewDto> createReview(
        @PathVariable UUID orderId,
        @RequestBody @Valid ReqCreateReviewDto request,
        @AuthenticationPrincipal UserPrincipal principal  // 추가
) {
    return ApiResponse.created(reviewService.createReview(orderId, request, principal));
}
```

---

## 새로 구현된 엔드포인트

### 2. 가게 리뷰 목록 조회

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/stores/{storeId}/reviews` |
| 인증 | 불필요 (공개) |
| 쿼리 파라미터 | `rating` (optional, 1~5), `page`, `size`, `sort` |
| 기본 정렬 | `createdAt DESC`, 페이지 사이즈 10 |

```java
@GetMapping("/stores/{storeId}/reviews")
public ApiResponse<Page<ResReviewDto>> getReviewsByStore(
        @PathVariable UUID storeId,
        @RequestParam(required = false) Integer rating,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
) {
    return ApiResponse.ok(reviewService.getReviewsByStore(storeId, rating, pageable));
}
```

서비스 로직:
- `rating` 값이 있으면 `findByStoreIdAndRating`, 없으면 `findByStoreId` 사용
- 결과를 `Page<ResReviewDto>`로 매핑하여 반환

---

### 3. 리뷰 수정

| 항목 | 내용 |
|---|---|
| 메서드 | `PATCH` |
| 경로 | `/api/v1/reviews/{reviewId}` |
| 인증 | 필요 (본인만) |
| 요청 바디 | `ReqUpdateReviewDto` (`rating`, `content`) |

```java
@PatchMapping("/reviews/{reviewId}")
public ApiResponse<ResReviewDto> updateReview(
        @PathVariable UUID reviewId,
        @RequestBody @Valid ReqUpdateReviewDto request,
        @AuthenticationPrincipal UserPrincipal principal
) {
    return ApiResponse.ok(reviewService.updateReview(reviewId, request, principal));
}
```

권한 검증:
```java
if (!review.getCustomerId().equals(principal.username())) {
    throw new BusinessException(ErrorCode.FORBIDDEN);
}
```

---

### 4. 리뷰 삭제

| 항목 | 내용 |
|---|---|
| 메서드 | `DELETE` |
| 경로 | `/api/v1/reviews/{reviewId}` |
| 인증 | 필요 |
| 응답 | 204 No Content |

```java
@DeleteMapping("/reviews/{reviewId}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void deleteReview(
        @PathVariable UUID reviewId,
        @AuthenticationPrincipal UserPrincipal principal
) {
    reviewService.deleteReview(reviewId, principal);
}
```

권한 검증 (소프트 딜리트):
- `MANAGER` / `MASTER` → 누구의 리뷰든 삭제 가능
- `CUSTOMER` / `OWNER` → 본인 리뷰만 삭제 가능

```java
boolean isAdminRole = principal.role() == UserRole.MANAGER || principal.role() == UserRole.MASTER;
if (!isAdminRole && !review.getCustomerId().equals(principal.username())) {
    throw new BusinessException(ErrorCode.FORBIDDEN);
}
review.delete(principal.username());
```

---

## SecurityConfig 변경

리뷰 쓰기 엔드포인트에 인증 규칙 추가:

```java
.requestMatchers(HttpMethod.POST, "/api/v1/orders/*/reviews").authenticated()
.requestMatchers(HttpMethod.PATCH, "/api/v1/reviews/*").authenticated()
.requestMatchers(HttpMethod.DELETE, "/api/v1/reviews/*").authenticated()
```

---

## TODO (Order/Store/User 구현 후 활성화)

| 위치 | 내용 |
|---|---|
| `createReview` | orderId로 Order 조회 후 storeId 추출 (`request.storeId()` 제거) |
| `createReview` | Order 완료 상태 검증 |
| `createReview` | 본인 주문 여부 검증 |
| `createReview`, `updateReview`, `deleteReview` | 가게 평균 평점 재집계 |
| `ResReviewDto` 반환 | "임시 가게명", "임시 닉네임" → 실제 조회로 교체 |
| `ReqCreateReviewDto` | `storeId` 필드 제거 (Order에서 추출) |