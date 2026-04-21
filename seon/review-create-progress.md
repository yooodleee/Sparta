# Review 작성 구현 진행 상황

> 작성일: 2026-04-17  
> 브랜치: `feature/1-implement-review-logic`

---

## 수정/생성된 파일

| 파일 | 변경 내용 |
|------|---------|
| `global/common/exception/ErrorCode.java` | 리뷰 에러 코드 3개 추가 |
| `review/presentation/dto/request/ReqCreateReviewDto.java` | 테스트용 임시 필드 추가 (`storeId`, `customerId`) |
| `review/application/service/ReviewService.java` | `createReview` 메서드 구현 |
| `review/presentation/controller/ReviewController.java` | `POST /orders/{orderId}/reviews` 엔드포인트 추가 |

---

## 추가된 에러 코드

```java
REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
DUPLICATE_REVIEW(HttpStatus.CONFLICT, "이미 해당 주문에 리뷰를 작성했습니다."),
ORDER_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "완료된 주문에만 리뷰를 작성할 수 있습니다.")
```

---

## 현재 구현된 검증 로직

| 검증 항목 | 상태 | 비고 |
|---------|------|------|
| 1주문 1리뷰 중복 검증 | ✅ 활성화 | `existsByOrderId` |
| Order 존재 여부 확인 | 💬 주석 처리 | Order 도메인 미구현 |
| 본인 주문 여부 확인 | 💬 주석 처리 | JWT 미구현 |
| 주문 완료 상태 확인 | 💬 주석 처리 | Order 도메인 미구현 |
| 가게 평균 평점 재집계 | 💬 주석 처리 | Store 도메인 미구현 |

---

## Postman 테스트

**요청**

```
POST http://localhost:8080/api/v1/orders/{orderId}/reviews
Content-Type: application/json
```

**Body**

```json
{
  "rating": 4,
  "content": "음식이 맛있었습니다!",
  "storeId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "user01"
}
```

> `orderId`는 임의의 UUID 사용 (예: `550e8400-e29b-41d4-a716-446655440001`)

**테스트 시나리오**

| 케이스 | 방법 | 기대 응답 |
|--------|------|---------|
| 정상 작성 | 새 `orderId` UUID 사용 | `201 CREATED` |
| 중복 리뷰 | 동일 `orderId`로 재요청 | `409 CONFLICT` |
| 평점 범위 초과 | `"rating": 6` | `400 BAD_REQUEST` |
| 평점 누락 | `rating` 필드 제거 | `400 BAD_REQUEST` |

---

## 나중에 교체할 TODO 목록

| 항목 | 교체 시점 | 위치 |
|------|---------|------|
| Order 조회 + 상태 검증 주석 해제 | Order 도메인 완성 후 | `ReviewService.createReview` |
| 본인 주문 검증 주석 해제 | JWT/Security 구현 후 | `ReviewService.createReview` |
| `storeId` DTO 필드 제거 | Order 도메인 완성 후 | `ReqCreateReviewDto` |
| `customerId` DTO 필드 제거 | JWT 구현 후 | `ReqCreateReviewDto` |
| `storeName` 실제 조회로 교체 | Store 도메인 완성 후 | `ReviewService.createReview` |
| `customerNickname` 실제 조회로 교체 | User 연동 후 | `ReviewService.createReview` |
| `recalculateAverageRating` 주석 해제 | Store 도메인 완성 후 | `ReviewService.createReview` |