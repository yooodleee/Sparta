# 본인 주문 확인 — JWT 인증 흐름

> 작성일: 2026-04-17

---

## 전체 흐름

```
클라이언트                    서버
   │                           │
   │  GET /api/v1/orders/123   │
   │  Authorization: Bearer {JWT}
   │ ─────────────────────────→│
   │                           │
   │               JwtAuthenticationFilter
   │                  1. JWT 파싱 → username, role 추출
   │                  2. DB에서 User 조회 → role 재검증
   │                  3. SecurityContext에 저장
   │                           │
   │                    OrderServiceV1
   │                  4. orderId로 Order 조회
   │                  5. order.customerId == SecurityContext의 username ?
   │                     → 같으면 OK / 다르면 403
   │                           │
   │  ←─────────────────────── │
```

---

## 코드로 보면

### 1단계 — JWT 필터가 SecurityContext에 인증 정보 저장

```java
// JwtAuthenticationFilter.java (Dev 1 담당)
String username = jwtTokenProvider.getUsername(token); // JWT에서 추출
UserEntity user = userRepository.findByUsername(username); // DB role 재검증

Authentication auth = new UsernamePasswordAuthenticationToken(
    username,           // principal — 이게 이후 @AuthenticationPrincipal로 꺼냄
    null,
    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
);
SecurityContextHolder.getContext().setAuthentication(auth);
```

### 2단계 — Controller에서 로그인 유저 꺼내기

```java
// OrderControllerV1.java
@GetMapping("/{orderId}")
public ApiResponse<ResOrderDto> getOrder(
    @PathVariable UUID orderId,
    @AuthenticationPrincipal String loginUsername  // SecurityContext에서 자동 주입
) {
    return ApiResponse.ok(orderService.getOrder(orderId, loginUsername));
}
```

### 3단계 — Service에서 본인 확인

```java
// OrderServiceV1.java
public ResOrderDto getOrder(UUID orderId, String loginUsername) {
    OrderEntity order = orderRepository.findById(orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

    // 권한별 접근 범위 분기
    UserRole role = getCurrentUserRole(); // SecurityContext에서 role도 꺼낼 수 있음

    if (role == CUSTOMER) {
        // CUSTOMER는 본인 주문만
        if (!order.getCustomerId().equals(loginUsername)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    } else if (role == OWNER) {
        // OWNER는 본인 가게 주문만
        if (!order.getStoreId().equals(getMyStoreId(loginUsername))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
    // MANAGER / MASTER는 통과

    return ResOrderDto.from(order);
}
```

---

## 역할별 접근 범위 정리

| 역할 | 무엇으로 확인 | 확인 방법 |
|------|------------|---------|
| **CUSTOMER** | 본인 주문인지 | `order.customerId == JWT의 username` |
| **OWNER** | 본인 가게 주문인지 | `order.storeId == 내 가게의 storeId` |
| **MANAGER / MASTER** | 제한 없음 | role만 확인하면 통과 |

---

## 핵심 요약

- **"누구인지"** → JWT가 담당 (필터에서 파싱 → SecurityContext 저장)
- **"그 사람이 이 주문을 볼 수 있는지"** → Service 레이어에서 직접 비교

JWT를 직접 Service에서 파싱하는 게 아니라, **필터가 미리 파싱해서 SecurityContext에 넣어두면**
Service는 `@AuthenticationPrincipal`로 username만 받아서 DB 데이터와 비교하는 방식.