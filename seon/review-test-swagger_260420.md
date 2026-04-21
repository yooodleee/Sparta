# Review API 테스트 & Swagger 가이드

작업 브랜치: `feature/1-implement-review-logic`
작업일: 2026-04-20

---

## 0. 사전 준비 - JWT 토큰 발급

모든 인증 필요 API 테스트 전에 먼저 테스트 유저를 생성해서 JWT를 받는다.
(실제 회원가입/로그인 구현 전까지 사용하는 임시 엔드포인트)

```
GET http://localhost:8080/api/v1/test/users
```

응답 예시:
```json
{
  "status": 200,
  "message": "SUCCESS",
  "data": {
    "username": "tabc12345",
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

이후 인증 필요 요청에는 헤더에 추가:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## 1. Postman 테스트

### 1-1. 리뷰 생성 - POST /api/v1/orders/{orderId}/reviews

#### 성공
```
POST http://localhost:8080/api/v1/orders/00000000-0000-0000-0000-000000000001/reviews
Authorization: Bearer {token}
Content-Type: application/json

{
  "rating": 5,
  "content": "맛있었어요!",
  "storeId": "00000000-0000-0000-0000-000000000099"
}
```
예상 응답: `201 Created`
```json
{
  "status": 201,
  "message": "CREATED",
  "data": {
    "reviewId": "...",
    "orderId": "00000000-0000-0000-0000-000000000001",
    "storeId": "00000000-0000-0000-0000-000000000099",
    "storeName": "임시 가게명",
    "customerId": "tabc12345",
    "customerNickname": "임시 닉네임",
    "rating": 5,
    "content": "맛있었어요!",
    "createdAt": "...",
    "updatedAt": "..."
  }
}
```

#### 실패 - 인증 없음 (토큰 미포함)
```
POST http://localhost:8080/api/v1/orders/{orderId}/reviews
(Authorization 헤더 없음)
```
예상 응답: `401 Unauthorized`

#### 실패 - 중복 리뷰 (같은 orderId로 재요청)
```
POST http://localhost:8080/api/v1/orders/00000000-0000-0000-0000-000000000001/reviews
Authorization: Bearer {token}

{
  "rating": 4,
  "storeId": "00000000-0000-0000-0000-000000000099"
}
```
예상 응답: `409 Conflict`
```json
{
  "status": 409,
  "message": "이미 해당 주문에 리뷰를 작성했습니다."
}
```

#### 실패 - Validation 오류 (rating 범위 초과)
```json
{
  "rating": 6,
  "storeId": "00000000-0000-0000-0000-000000000099"
}
```
예상 응답: `400 Bad Request`

#### 실패 - storeId 누락
```json
{
  "rating": 5
}
```
예상 응답: `400 Bad Request`

---

### 1-2. 가게 리뷰 목록 조회 - GET /api/v1/stores/{storeId}/reviews

#### 성공 - 전체 목록 (인증 불필요)
```
GET http://localhost:8080/api/v1/stores/00000000-0000-0000-0000-000000000099/reviews
```
예상 응답: `200 OK` (Page 형태)
```json
{
  "status": 200,
  "message": "SUCCESS",
  "data": {
    "content": [...],
    "totalElements": 1,
    "totalPages": 1,
    "size": 10,
    "number": 0
  }
}
```

#### 성공 - 평점 필터
```
GET http://localhost:8080/api/v1/stores/{storeId}/reviews?rating=5
```

#### 성공 - 페이지네이션
```
GET http://localhost:8080/api/v1/stores/{storeId}/reviews?page=0&size=5
```

---

### 1-3. 리뷰 수정 - PATCH /api/v1/reviews/{reviewId}

#### 성공 (리뷰 작성자 본인 토큰)
```
PATCH http://localhost:8080/api/v1/reviews/{reviewId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "rating": 3,
  "content": "다시 생각해보니 보통이었어요"
}
```
예상 응답: `200 OK`

#### 실패 - 타인의 리뷰 수정 시도
다른 유저 토큰(`GET /api/v1/test/users`로 새 유저 생성)으로 요청
예상 응답: `403 Forbidden`

#### 실패 - 인증 없음
예상 응답: `401 Unauthorized`

#### 실패 - 존재하지 않는 reviewId
```
PATCH http://localhost:8080/api/v1/reviews/00000000-0000-0000-0000-000000000000
```
예상 응답: `404 Not Found`

---

### 1-4. 리뷰 삭제 - DELETE /api/v1/reviews/{reviewId}

#### 성공 - 본인 삭제
```
DELETE http://localhost:8080/api/v1/reviews/{reviewId}
Authorization: Bearer {token}
```
예상 응답: `204 No Content`

#### 성공 - MASTER 권한으로 타인 리뷰 삭제
TestAuthService가 MASTER 역할 유저를 생성하므로, 기본 테스트 토큰으로 누구 리뷰든 삭제 가능

#### 실패 - 인증 없음
예상 응답: `401 Unauthorized`

#### 실패 - 존재하지 않는 reviewId
예상 응답: `404 Not Found`

---

## 2. 테스트 코드

`spring-boot-starter-test`가 이미 포함되어 있어 JUnit 5 + Mockito + MockMvc 바로 사용 가능.
DB(PostgreSQL)가 필요 없는 **컨트롤러 단위 테스트** 방식으로 작성한다.

### build.gradle - 추가 의존성 없음 (이미 포함)
```groovy
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
```

### 테스트 파일 위치
```
src/test/java/com/example/delivery/review/presentation/controller/ReviewControllerTest.java
```

### 테스트 코드 전체
```java
package com.example.delivery.review.presentation.controller;

import com.example.delivery.global.infrastructure.security.UserPrincipal;
import com.example.delivery.review.application.service.ReviewService;
import com.example.delivery.review.presentation.dto.request.ReqCreateReviewDto;
import com.example.delivery.review.presentation.dto.request.ReqUpdateReviewDto;
import com.example.delivery.review.presentation.dto.response.ResReviewDto;
import com.example.delivery.user.domain.entity.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ReviewService reviewService;

    private static final UUID ORDER_ID  = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();
    private static final UUID REVIEW_ID = UUID.randomUUID();

    private final UserPrincipal principal =
            new UserPrincipal(1L, "testuser", UserRole.CUSTOMER);

    private UsernamePasswordAuthenticationToken auth(UserPrincipal p) {
        return new UsernamePasswordAuthenticationToken(
                p, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + p.role().name())));
    }

    private ResReviewDto sampleDto() {
        return new ResReviewDto(
                REVIEW_ID, ORDER_ID, STORE_ID,
                "임시 가게명", "testuser", "임시 닉네임",
                5, "맛있었어요!", LocalDateTime.now(), LocalDateTime.now());
    }

    // ── createReview ─────────────────────────────────────────────

    @Test
    @DisplayName("리뷰 생성 - 성공")
    void createReview_success() throws Exception {
        ReqCreateReviewDto req = new ReqCreateReviewDto(5, "맛있었어요!", STORE_ID);
        given(reviewService.createReview(eq(ORDER_ID), any(), any()))
                .willReturn(sampleDto());

        mockMvc.perform(post("/api/v1/orders/{orderId}/reviews", ORDER_ID)
                        .with(authentication(auth(principal)))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.rating").value(5));
    }

    @Test
    @DisplayName("리뷰 생성 - 실패: 인증 없음 (401)")
    void createReview_unauthorized() throws Exception {
        ReqCreateReviewDto req = new ReqCreateReviewDto(5, "맛있었어요!", STORE_ID);

        mockMvc.perform(post("/api/v1/orders/{orderId}/reviews", ORDER_ID)
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("리뷰 생성 - 실패: 중복 리뷰 (409)")
    void createReview_duplicate() throws Exception {
        ReqCreateReviewDto req = new ReqCreateReviewDto(5, "맛있었어요!", STORE_ID);
        given(reviewService.createReview(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.DUPLICATE_REVIEW));

        mockMvc.perform(post("/api/v1/orders/{orderId}/reviews", ORDER_ID)
                        .with(authentication(auth(principal)))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("리뷰 생성 - 실패: rating 범위 초과 (400)")
    void createReview_invalidRating() throws Exception {
        ReqCreateReviewDto req = new ReqCreateReviewDto(6, null, STORE_ID);

        mockMvc.perform(post("/api/v1/orders/{orderId}/reviews", ORDER_ID)
                        .with(authentication(auth(principal)))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── getReviewsByStore ─────────────────────────────────────────

    @Test
    @DisplayName("가게 리뷰 목록 조회 - 성공 (인증 불필요)")
    void getReviewsByStore_success() throws Exception {
        Page<ResReviewDto> page = new PageImpl<>(
                List.of(sampleDto()), PageRequest.of(0, 10), 1);
        given(reviewService.getReviewsByStore(eq(STORE_ID), eq(null), any()))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/stores/{storeId}/reviews", STORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("가게 리뷰 목록 조회 - 평점 필터")
    void getReviewsByStore_ratingFilter() throws Exception {
        Page<ResReviewDto> page = new PageImpl<>(List.of(sampleDto()), PageRequest.of(0, 10), 1);
        given(reviewService.getReviewsByStore(eq(STORE_ID), eq(5), any()))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/stores/{storeId}/reviews", STORE_ID)
                        .param("rating", "5"))
                .andExpect(status().isOk());
    }

    // ── updateReview ──────────────────────────────────────────────

    @Test
    @DisplayName("리뷰 수정 - 성공")
    void updateReview_success() throws Exception {
        ReqUpdateReviewDto req = new ReqUpdateReviewDto(3, "다시 생각해보니 보통이었어요");
        given(reviewService.updateReview(eq(REVIEW_ID), any(), any()))
                .willReturn(sampleDto());

        mockMvc.perform(patch("/api/v1/reviews/{reviewId}", REVIEW_ID)
                        .with(authentication(auth(principal)))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("리뷰 수정 - 실패: 타인 리뷰 (403)")
    void updateReview_forbidden() throws Exception {
        ReqUpdateReviewDto req = new ReqUpdateReviewDto(3, "수정");
        given(reviewService.updateReview(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(patch("/api/v1/reviews/{reviewId}", REVIEW_ID)
                        .with(authentication(auth(principal)))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("리뷰 수정 - 실패: 존재하지 않는 리뷰 (404)")
    void updateReview_notFound() throws Exception {
        ReqUpdateReviewDto req = new ReqUpdateReviewDto(3, "수정");
        given(reviewService.updateReview(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/reviews/{reviewId}", REVIEW_ID)
                        .with(authentication(auth(principal)))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // ── deleteReview ──────────────────────────────────────────────

    @Test
    @DisplayName("리뷰 삭제 - 성공")
    void deleteReview_success() throws Exception {
        willDoNothing().given(reviewService).deleteReview(eq(REVIEW_ID), any());

        mockMvc.perform(delete("/api/v1/reviews/{reviewId}", REVIEW_ID)
                        .with(authentication(auth(principal)))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("리뷰 삭제 - 실패: 인증 없음 (401)")
    void deleteReview_unauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/reviews/{reviewId}", REVIEW_ID)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("리뷰 삭제 - 실패: 타인 리뷰 (403)")
    void deleteReview_forbidden() throws Exception {
        willThrow(new BusinessException(ErrorCode.FORBIDDEN))
                .given(reviewService).deleteReview(any(), any());

        mockMvc.perform(delete("/api/v1/reviews/{reviewId}", REVIEW_ID)
                        .with(authentication(auth(principal)))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
```

### 주의사항
- `@WebMvcTest`는 **컨트롤러 레이어만** 띄운다 → DB 연결 불필요
- `ReviewService`는 `@MockitoBean`으로 가짜 객체 주입
- CSRF는 `SecurityConfig`에서 비활성화했지만, `@WebMvcTest`는 Spring Security를 기본 활성화하므로 `.with(csrf())` 처리 필요
- 인증은 `.with(authentication(...))` 로 직접 주입 (JWT 필터를 거치지 않음)
- `spring-security-test`가 `spring-boot-starter-test`에 포함되어 있어 별도 의존성 추가 불필요

---

## 3. Swagger (Springdoc OpenAPI)

### 테스트 먼저? Swagger 먼저?

**테스트를 먼저 마치고 Swagger를 붙이는 게 맞다.**

이유:
- Swagger는 문서화 도구이지 테스트 도구가 아님
- 로직이 확정되지 않은 상태에서 어노테이션을 달면 API 변경 시 문서도 같이 수정해야 해서 이중 작업
- 기본 설정만으로도 어느 정도 자동 문서가 생성되므로, 안정화 후 필요한 부분만 어노테이션으로 보완하면 됨

### 의존성 추가 (build.gradle)

```groovy
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0'
```

> Spring Boot 3.x 에는 반드시 `springdoc-openapi-starter-*` (2.x) 사용.  
> 구버전 `springdoc-openapi-ui` (1.x) 는 Spring Boot 3과 호환 안됨.

### 접속 URL

```
http://localhost:8080/swagger-ui/index.html   # UI
http://localhost:8080/v3/api-docs             # JSON 스펙
```

### SecurityConfig - Swagger 경로 허용 추가

```java
private static final String[] PUBLIC_PATHS = {
        "/actuator/health",
        "/actuator/info",
        "/error",
        "/api/v1/test/users",
        "/swagger-ui/**",       // 추가
        "/v3/api-docs/**"       // 추가
};
```

### 선택적으로 추가하는 어노테이션 (안정화 후)

어노테이션 없어도 자동으로 문서가 생성되지만, 아래를 달면 더 명확해진다:

```java
// Controller 클래스
@Tag(name = "Review", description = "리뷰 API")

// 각 메서드
@Operation(summary = "리뷰 생성", description = "주문에 대한 리뷰를 작성합니다.")
@ApiResponse(responseCode = "201", description = "리뷰 생성 성공")
@ApiResponse(responseCode = "401", description = "인증 필요")
@ApiResponse(responseCode = "409", description = "중복 리뷰")
```

### 작업 순서 권장

```
1. 구현 완료
2. Postman 테스트 → 성공/실패 케이스 확인
3. 테스트 코드 작성 → 통과 확인
4. build.gradle에 springdoc 의존성 추가
5. SecurityConfig PUBLIC_PATHS에 swagger 경로 추가
6. 앱 실행 후 swagger-ui 접속해서 자동 문서 확인
7. 필요한 경우에만 @Operation, @ApiResponse 어노테이션 추가
```