# 08. 애플리케이션 흐름

> 이전: [코드 레벨 설계](07-code-design.md)

애플리케이션 런타임 관점의 호출 흐름·시퀀스 모음.

---

## 0. 런타임 컴포넌트 도식

요청이 논리 컴포넌트를 따라 어떻게 흐르는지의 전반적인 그림. 주요 유스케이스별 시퀀스는 다음 섹션에서 상세히 다룬다.

```mermaid
flowchart LR
    Client["클라이언트<br/>(Postman / Swagger UI)"]
    subgraph App["Spring Boot App (Monolith)"]
        Filter["JwtAuthenticationFilter"]
        Controller["@RestController"]
        Service["@Service<br/>(@Transactional)"]
        Repo["Repository (JPA / QueryDSL)"]
        subgraph Ext["외부 연동 어댑터 (Strategy · Profile 기반 주입)"]
            IExt["«interface»<br/>*Client<br/>(예: AiClient · PaymentClient ...)"]
            Real["Real 구현체<br/>(profile = prod)"]
            Fake["Fake 구현체<br/>(profile = local / test)"]
        end
    end
    DB[("PostgreSQL<br/>(p_user · p_order · ...)")]
    External["외부 시스템<br/>(예: Gemini · PG ...)"]
    Client -->|" HTTPS + Bearer JWT "| Filter
    Filter --> Controller
    Controller --> Service
    Service --> Repo
    Repo -->|" JDBC "| DB
    Service -->|" uses "| IExt
    Real -.->|" impl "| IExt
    Fake -.->|" impl "| IExt
    Real -->|" HTTPS REST "| External
```

- 검증 레이어: Controller는 `@NotBlank`/`@Size` 등 **물리 Validation**, 도메인 객체/Service는 **논리 규칙**(정규식·범위·상태 전이)
- `JwtAuthenticationFilter`는 매 요청마다 DB에서 현재 `role` / `deleted_at`을 재조회해 토큰 payload와 대조
- **외부 연동 어댑터**: AI · PG 등 외부 시스템 호출은 `*Client` 인터페이스 + Real/Fake 구현체 2종 패턴 사용 고려.
- 외부 호출 로그(예: `AiRequestLog`)는 Service 레이어에서 성공/실패 모두

## 1. 인증 파이프라인

로그인으로 JWT를 발급받고, 이후 요청마다 Filter가 **DB에서 role/삭제 여부를 재검증**하는 두 단계 흐름.

```mermaid
sequenceDiagram
    autonumber
    actor Client as 클라이언트
    participant Auth as AuthController / Service
    participant UR as UserRepository
    participant JTP as JwtTokenProvider
    participant F as JwtAuthenticationFilter
    participant BC as Business Layer
    Note over Client,JTP: 1단계 · 로그인 (JWT 발급)
    Client ->> Auth: POST /auth/login
    Auth ->> UR: findByUsername + BCrypt.matches
    UR -->> Auth: UserEntity
    Auth ->> JTP: generate(username, role)
    JTP -->> Auth: JWT (HS256)
    Auth -->> Client: 200 OK + JWT
    Note right of Auth: 실패 시 401<br/>(유저 없음 · 비밀번호 불일치)
    Note over Client,BC: 2단계 · 인증된 요청 (매 요청 DB 재검증)
    Client ->> F: Bearer JWT + 요청
    F ->> JTP: parse(token)
    JTP -->> F: claims(username, role)
    F ->> UR: findByUsername(username)
    UR -->> F: UserEntity
    F ->> BC: SecurityContext 주입 후 chain 진행
    BC -->> Client: 200 OK
    Note right of F: 실패 시<br/>• JWT 만료 / 파싱 실패 → 401<br/>• 유저 없음 / soft-deleted → 401<br/>• payload.role ≠ DB.role → 403
```

**설계 포인트**

- **매 요청 DB 재검증**: JWT payload만 신뢰하지 않고 `UserRepository.findByUsername`으로 현재 `role`·`deleted_at`을 확인. 권한 변경/탈퇴 시 기존 토큰이
  **즉시 무력화**되는 효과 (별도 블랙리스트 불필요).
    - 트레이드오프: 모든 요청이 DB 1회 조회를 추가로 발생시킴. 부하 커지면 Redis 캐시 고려
- **401 vs 403 구분**: "본인 확인 실패" = 401 (토큰 파싱 실패, 유저 없음, 삭제됨), "본인은 맞지만 권한 부족" = 403 (payload.role ≠ DB.role).

## 2. 주문 생성

CUSTOMER가 주문을 생성하는 흐름. 컨트롤러 진입 이후 서비스는 트랜잭션 경계·조립만 담당하고, 상태 전이/불변식은 `OrderEntity`가 스스로 검증한다.

```mermaid
sequenceDiagram
  autonumber
  actor Client as 클라이언트 (CUSTOMER)
  participant F as JwtAuthenticationFilter
  participant C as OrderControllerV1
  participant S as OrderServiceV1
  participant OE as OrderEntity<br/>/OrderItemEntity
  participant R as OrderRepository
  participant DB as PostgreSQL

  Note over Client,F: 인증 파이프라인은 §1 참조

  Client ->> F: 주문 생성 요청
  F ->> C: 인증 통과 후 컨트롤러로 위임
  C ->> C: 요청 DTO 물리 검증
  C ->> S: 서비스 호출 (트랜잭션 시작)

  S ->> S: 주문 총액 계산 (스냅샷)
  S ->> OE: 주문 엔티티 생성

  loop 각 주문 항목 반복
    S ->> OE: 주문 항목 엔티티 생성
    Note right of OE: 엔티티 불변식 검증
    S ->> OE: 주문-항목 연관관계 연결
  end

  S ->> R: 주문 영속화 요청
  R ->> DB: 주문 + 항목 INSERT (cascade)
  DB -->> R: 영속 엔티티 반환
  R -->> S: 저장된 주문 반환
  S -->> C: 응답 DTO 변환 (트랜잭션 커밋)
  C -->> Client: 생성된 주문 응답

  Note right of S: 실패 시 VALIDATION_ERROR 등으로<br/>예외 응답 반환
```

**설계 포인트**

- **주문 시점 스냅샷**: `totalPrice`는 서비스가 클라이언트 입력 `quantity × unitPrice`로 계산해 저장. 이후 메뉴 가격이 바뀌거나 메뉴가 삭제되어도 과거 주문 금액은 불변.
- **물리 검증(@Valid) vs 논리 불변식**: DTO 레이어에서 `@NotNull`/`@Min` 같은 물리 검증을 먼저 걷어내고, `OrderItemEntity` 생성자가 `quantity > 0` / `unitPrice ≥ 0` 논리 불변식을 한 번 더 방어한다.
- **Cascade ALL + orphanRemoval**: `OrderItemEntity`는 별도 save 호출 없이 `addItem()` → `orderRepository.save(order)` 한 번으로 함께 영속화된다.
- **도메인 로직 위임**: 서비스는 트랜잭션 경계·조립만 담당하고, 상태 전이/불변식 검증은 `OrderEntity`가 스스로 수행한다 (DDD 관점).
- **Soft Delete**: `@SQLRestriction("deleted_at IS NULL")` 로 조회 시 자동 제외.

## 3. 결제 (Fake / 실 PG)

> **TBD**

## 4. 메뉴 생성 + AI 설명

> **TBD**