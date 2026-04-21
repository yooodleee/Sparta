# Review 도메인 구현 계획

> 작성일: 2026-04-17  
> 우선순위: P1 (Auth/User/Store/Order 완성 후 진행)  
> 기능 명세 참조: `docs/01-functional-spec.md` § 2.8 (F-REV-01 ~ F-REV-04)

---

## 1. 사전 조건

Review를 구현하기 전에 **반드시** 아래 도메인이 완성되어 있어야 한다.

| 사전 도메인 | 필요 이유 |
|-----------|---------|
| User (JWT 포함) | `customer_id` FK, 작성자 인증 |
| Store | `store_id` FK, `average_rating` 재집계 |
| Order | `order_id` FK, `COMPLETED` 상태 검증 |

---

## 2. 기능 명세 요약

| ID | 기능 | 권한 | 핵심 제약 |
|----|------|------|---------|
| F-REV-01 | 리뷰 작성 | CUSTOMER (본인 주문) | `Order.status = COMPLETED`, 1주문 1리뷰 |
| F-REV-02 | 리뷰 조회 | ALL | `storeId`, `rating` 필터, 페이지네이션 |
| F-REV-03 | 리뷰 수정/삭제 | 본인(수정·삭제) / MANAGER·MASTER(삭제만) | Soft Delete |
| F-REV-04 | 가게 평균 평점 재집계 | 시스템 (CUD 시 자동) | 동일 트랜잭션 내 처리 |

---

## 3. 데이터 모델

### 3.1 `p_review` 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `review_id` | UUID | PK | |
| `order_id` | UUID | FK → p_order, **UNIQUE**, NOT NULL | 1주문 1리뷰 보장 |
| `store_id` | UUID | FK → p_store, NOT NULL | 역정규화 (가게별 조회 성능) |
| `customer_id` | VARCHAR(10) | FK → p_user.username, NOT NULL | |
| `rating` | INTEGER | NOT NULL, CHECK 1 ≤ rating ≤ 5 | |
| `content` | TEXT | | |
| + Audit 6개 | | | BaseEntity 상속 |

### 3.2 인덱스

```sql
-- 가게별 리뷰 최신순 조회 성능
CREATE INDEX idx_review_store_created ON p_review (store_id, created_at DESC);
```

---

## 4. 구현할 파일 목록

```
src/main/java/com/example/delivery/review/
├── domain/
│   ├── entity/
│   │   └── ReviewEntity.java           # @Entity, BaseEntity 상속
│   └── repository/
│       └── ReviewRepository.java       # 도메인 계층 인터페이스
├── infrastructure/
│   └── repository/
│       └── ReviewJpaRepository.java    # JpaRepository 구현
├── application/
│   └── service/
│       └── ReviewServiceV1.java        # 비즈니스 로직
└── presentation/
    ├── controller/
    │   └── ReviewControllerV1.java     # REST 엔드포인트
    └── dto/
        ├── request/
        │   ├── ReqCreateReviewDto.java
        │   └── ReqUpdateReviewDto.java
        └── response/
            └── ResReviewDto.java
```

---

## 5. 엔드포인트 설계

| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| `POST` | `/api/v1/orders/{orderId}/reviews` | 리뷰 작성 | CUSTOMER (본인 주문, COMPLETED) |
| `GET` | `/api/v1/reviews` | 목록 조회 (storeId, rating 필터) | ALL |
| `GET` | `/api/v1/reviews/{reviewId}` | 상세 조회 | ALL |
| `PUT` | `/api/v1/reviews/{reviewId}` | 리뷰 수정 | CUSTOMER (본인) |
| `DELETE` | `/api/v1/reviews/{reviewId}` | 리뷰 삭제 (Soft Delete) | 본인 / MANAGER / MASTER |

---

## 6. 단계별 구현 계획

### Step 1: 엔티티 및 리포지토리

**`ReviewEntity.java`**

```java
@Entity
@Table(name = "p_review",
    indexes = @Index(name = "idx_review_store_created",
                     columnList = "store_id, created_at DESC"))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")  // Soft Delete 자동 필터
public class ReviewEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID reviewId;

    @Column(nullable = false, unique = true)  // 1주문 1리뷰
    private UUID orderId;

    @Column(nullable = false)
    private UUID storeId;

    @Column(length = 10, nullable = false)
    private String customerId;

    @Column(nullable = false)
    private int rating;  // 1~5

    @Column(columnDefinition = "TEXT")
    private String content;

    // 수정 메서드
    public void update(int rating, String content) {
        this.rating = rating;
        this.content = content;
    }

    // Soft Delete
    public void delete(String deletedBy) {
        markDeleted(deletedBy);  // BaseEntity 메서드
    }
}
```

**`ReviewJpaRepository.java`**

```java
public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, UUID> {
    boolean existsByOrderId(UUID orderId);
    Page<ReviewEntity> findByStoreId(UUID storeId, Pageable pageable);
    Page<ReviewEntity> findByStoreIdAndRating(UUID storeId, int rating, Pageable pageable);

    // 평균 평점 재집계용
    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.storeId = :storeId")
    Optional<Double> calculateAverageRating(@Param("storeId") UUID storeId);
}
```

---

### Step 2: 서비스 로직

**`ReviewServiceV1.java`** 핵심 메서드 설계

#### 2-1. 리뷰 작성 (`createReview`)

```
검증 흐름:
1. orderId로 Order 조회 → 없으면 404
2. order.customerId == 현재 로그인 유저 → 아니면 403
3. order.status == COMPLETED → 아니면 400 (INVALID_ORDER_STATUS)
4. existsByOrderId(orderId) → true면 409 (DUPLICATE_REVIEW)
5. ReviewEntity 저장
6. Store.averageRating 재집계 (동일 트랜잭션)
```

#### 2-2. 리뷰 수정 (`updateReview`)

```
검증 흐름:
1. reviewId로 Review 조회 → 없으면 404
2. review.customerId == 현재 로그인 유저 → 아니면 403
3. rating, content 업데이트
4. Store.averageRating 재집계 (동일 트랜잭션)
```

#### 2-3. 리뷰 삭제 (`deleteReview`)

```
검증 흐름:
1. reviewId로 Review 조회 → 없으면 404
2. 권한 확인:
   - CUSTOMER: review.customerId == 현재 유저 → 아니면 403
   - MANAGER / MASTER: 무조건 허용
3. review.delete(현재 유저명)  → Soft Delete
4. Store.averageRating 재집계 (동일 트랜잭션)
```

#### 2-4. 평균 평점 재집계 (`recalculateAverageRating`) — private 내부 메서드

```java
// ReviewService 내부 호출용
private void recalculateAverageRating(UUID storeId) {
    double avg = reviewJpaRepository
        .calculateAverageRating(storeId)
        .orElse(0.0);
    storeRepository.updateAverageRating(storeId, avg);  // Store 도메인 의존
}
```

> **주의**: CUD 시 매번 `recalculateAverageRating`를 호출해야 한다.  
> 리뷰가 0개가 되면 평균을 0.0으로 초기화한다.

---

### Step 3: DTO 설계

**`ReqCreateReviewDto.java`**

```java
public record ReqCreateReviewDto(
    @NotNull @Min(1) @Max(5)
    Integer rating,

    @Size(max = 1000)
    String content
) {}
```

**`ReqUpdateReviewDto.java`**

```java
public record ReqUpdateReviewDto(
    @NotNull @Min(1) @Max(5)
    Integer rating,

    @Size(max = 1000)
    String content
) {}
```

**`ResReviewDto.java`**

```java
public record ResReviewDto(
    UUID reviewId,
    UUID orderId,
    UUID storeId,
    String customerId,
    int rating,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ResReviewDto from(ReviewEntity entity) { ... }
}
```

---

### Step 4: 컨트롤러 구현

```java
@RestController
@RequiredArgsConstructor
public class ReviewControllerV1 {

    // POST /api/v1/orders/{orderId}/reviews
    @PostMapping("/api/v1/orders/{orderId}/reviews")

    // GET /api/v1/reviews?storeId=&rating=&page=&size=&sort=
    @GetMapping("/api/v1/reviews")

    // GET /api/v1/reviews/{reviewId}
    @GetMapping("/api/v1/reviews/{reviewId}")

    // PUT /api/v1/reviews/{reviewId}
    @PutMapping("/api/v1/reviews/{reviewId}")

    // DELETE /api/v1/reviews/{reviewId}
    @DeleteMapping("/api/v1/reviews/{reviewId}")
}
```

---

### Step 5: 에러 코드 추가

`global/common/exception/ErrorCode.java`에 아래 값 추가 필요:

```java
DUPLICATE_REVIEW(HttpStatus.CONFLICT, "이미 해당 주문에 리뷰를 작성했습니다."),
REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
ORDER_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "완료된 주문에만 리뷰를 작성할 수 있습니다."),
```

---

### Step 6: 테스트 코드

#### 6-1. Repository 테스트 (`@DataJpaTest`)
- `existsByOrderId` — 중복 리뷰 검출
- `findByStoreId` — 가게별 목록 페이지네이션
- `calculateAverageRating` — 평균 계산 정확도

#### 6-2. Service 테스트 (`@ExtendWith(MockitoExtension.class)`)
- 리뷰 작성 성공
- 작성 실패: `Order.status != COMPLETED`
- 작성 실패: 중복 리뷰 (`DUPLICATE_REVIEW`)
- 작성 실패: 본인 주문 아님 (`FORBIDDEN`)
- 삭제 성공: 본인 CUSTOMER
- 삭제 성공: MANAGER
- 삭제 실패: 타인 CUSTOMER
- CUD 후 `average_rating` 재집계 검증

#### 6-3. Controller 테스트 (`@WebMvcTest`)
- 유효하지 않은 `rating` (0, 6) → 400 VALIDATION_ERROR
- 인증 토큰 없음 → 401 UNAUTHORIZED
- 권한 부족 → 403 FORBIDDEN

---

## 7. 유의사항 및 체크리스트

- [ ] `@SQLRestriction("deleted_at IS NULL")` — 삭제된 리뷰가 조회에서 자동 제외되는지 확인
- [ ] 리뷰 삭제 후 `average_rating`이 **0.0**으로 올바르게 초기화되는지 확인 (리뷰 0개일 때)
- [ ] `store_id`는 역정규화 컬럼 — Order에서 Store를 따라가지 말고 Order 조회 시 `storeId`를 직접 추출
- [ ] 페이지 사이즈는 10/30/50만 허용, 그 외 요청은 10으로 강제
- [ ] 기본 정렬: `createdAt DESC`
- [ ] 리뷰 조회 API는 Soft Delete된 리뷰를 응답에 포함하지 않아야 함
- [ ] `average_rating` 재집계는 반드시 **같은 트랜잭션** 내에서 처리 (`@Transactional`)