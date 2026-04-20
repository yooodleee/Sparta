# 팀 협업 및 기술 컨벤션 (초안)

> 상태: DRAFT — 일부 항목은 팀 합의 필요
---

## 1. PR 및 코드 리뷰 프로세스

- PR 단위: 기능/Issue 단위 분할, 세부 크기는 작성자 판단에 맡기되 다른 사람이 PR 확인 시 불편하지 않을 정도로 유지
- 머지 조건: 2명 Approve + CI 그린 + 컨플릭트 해소 (머지 실행은 PR 작성자 본인이 수행)
- 리뷰 SLA: 일과 끝났을 때?
- CI: GitHub Actions로 `./gradlew build test` 자동 실행 (브랜치 푸시 + PR open 트리거)
  - 작성된 테스트 코드의 통과 여부만 검증, 커버리지 기준은 두지 않음
- 머지 전략: Squash 미사용, 모든 커밋 이력이 남는 일반 Merge 방식 사용
- 머지 후: issue 자동 close, 브랜치 즉시 삭제

---

## 2. 문서화 체계

- 설계 문서: `docs/00~07-*.md` (변경 시 동일 PR에 포함)
- API 문서: Swagger UI 채택 — `@Operation`, `@Schema` 어노테이션을 컨트롤러/DTO에 인라인. RestDocs는 사용하지 않음
- README: 로컬 셋업, `./gradlew bootRun`, `docker-compose up`, 환경변수 목록만, 추후 대표 기능·아키텍처 요약 추가 검토
- 이슈 추적: 작업·버그·개선 사항은 모두 GitHub Issue 발행으로 시작. 해결 내역(원인 분석, 접근 방식, 대안 비교, 트레이드오프)은 PR description에 상세 기록
    - 별도 문서화를 하지 않고, 최대한 GitHub 이슈·PR description에 모든 내용을 담는 방향으로 (문서와 이슈 간 중복 방지)

---

## 3. 아키텍처 및 개발 컨벤션

### 3.1 패키지 구조 ([`07-code-design.md`](07-code-design.md) 따름)

```
com.example.delivery
├── global/infrastructure/{config, presentation/advice, entity}
└── {domain}/
    ├── application/service/{Domain}ServiceV1
    ├── domain/{entity, repository}
    ├── infrastructure/repository/...Custom
    └── presentation/{controller, dto/{request,response}}
```

- 의존성 흐름: `presentation → application → domain`
- 도메인간 의존성 흐름 논의 필요

### 3.2 JPA 및 데이터 모델링

| 항목          | 규칙                                                       |
|-------------|----------------------------------------------------------|
| FetchType   | EAGER 금지, 모든 연관관계에 `fetch = FetchType.LAZY` 명시           |
| 매핑 방향       | 단방향 기본, 양방향은 애그리거트 내부만 필요한 경우                            |
| 도메인 간 참조    | ID 참조                                                    |
| 도메인 내 참조    | 객체 참조 OK(Order <-> OrderItem)                            |
| Cascade     | 금지가 원칙이나 애그리거트 내부는 필요한 경우 허용                             |
| Soft Delete | `BaseEntity.deletedAt` 사용, 쿼리에 `deleted_at IS NULL` 필터   |
| Audit       | `BaseEntity` 상속 (예외: `p_order_item`, `p_ai_request_log`) |
| PK          | UUID v4 (최초 요구 사항)                                       |
| N+1         | `@EntityGraph` 또는 fetch join, 페이징 시 `@BatchSize`         |

### 3.3 데이터 교환 (DTO)

- Entity는 Controller 경계 밖으로 절대 노출 금지 (요청·응답·직렬화 모두 DTO 사용)
- 네이밍: `Req{Action}{Domain}Dto` / `Res{Domain}Dto`
- 변환 위치: Controller에서 Entity↔DTO 변환 (Service는 도메인 객체 다룸)
- 공통 응답: `ApiResponse<T>` 래퍼
- 페이지: `PageResponse<T>` 통일(생성 필요)

### 3.4 예외 처리

- `BusinessException` + `ErrorCode` enum 기반, `GlobalExceptionHandler`에서 일괄 변환
- Controller에서 try/catch 금지 (Validation, BusinessException, 그 외로 매핑)

### 3.5 테스트 ([`07-code-design.md`](07-code-design.md))

- Repository: 미작성
- Service: Mockito mock or 인터페이스 기반 Fake Mock 객체 직접 구현 — 성공·실패 케이스 필수
- Controller: `@WebMvcTest` + MockMvc — 권한·Validation 검증

### 3.6 보안

- 매 요청 시 JWT 파싱 → role 검증 + UserID 추출
- 비밀번호: BCrypt
- 민감 정보 로깅 금지 (password, token)

### 3.7 순환 참조 방지

도메인 간 Service ↔ Service 직접 호출이 누적되면 양방향 의존이 발생하기 쉽다.

- `Order` ↔ `Menu` (주문 생성 시 메뉴 가격 조회) ↔ `Store` (가게 검증)
- `Review` ↔ `Order` (리뷰 작성 권한 체크) ↔ `Store` (평점 재계산)
- `Menu` ↔ `Ai` (AI 설명 생성)

해결 방식 후보 — 하나를 표준으로 정해두는 것이 좋음 (혼용 시 코드베이스 일관성 깨짐):

| 옵션 | 방식                                                                            | 장점                                       | 단점                               |
|----|-------------------------------------------------------------------------------|------------------------------------------|----------------------------------|
| A  | Facade 계층 신설<br>(`{domain}/application/facade/OrderFacade`)                   | 도메인 Service는 단일 책임 유지, 조합 로직만 Facade에 격리 | 클래스 수 증가, Facade가 비대해질 수 있음      |
| B  | 상위 Service가 하위 Service를 단방향 호출<br>(예: `OrderService` → `MenuService`, 역방향 금지) | 추가 클래스 없음, 단순                            | "상위/하위" 합의 필요. 의존 방향 깨지면 즉시 순환   |
| C  | Spring 도메인 이벤트<br>(`ApplicationEventPublisher` + `@EventListener`)            | 완전한 비결합, 트랜잭션 전후 처리 분리 가능                | 흐름 추적 어려움, 트랜잭션 경계·실패 처리 룰 추가 필요 |

### 3.8 도메인 로직 vs 서비스 로직

| 위치                                       | 작성하는 것                                                                                              |
|------------------------------------------|-----------------------------------------------------------------------------------------------------|
| Entity (`{domain}/domain/entity`)        | 상태 변경 메서드, 불변식 검증, 자기 필드만으로 결정되는 계산 (예: `Order.cancelByCustomer`, `Store.recalculateAverageRating`) |
| Service (`{domain}/application/service`) | 트랜잭션 경계, 권한 검증, Repository 호출, 외부 API 호출, Entity 메서드 오케스트레이션                                        |
| Facade (`{domain}/application/facade`)   | 여러 도메인 Service 조합(계층 사용시)                                                                           |

- Entity(도메인)에 setter 금지, 도메인 의도가 드러나는 메서드로 상태 변경(setStatus 대신 `accept()`, `startCooking()` 등)
- 서비스는 도메인 객체를 조작하는 데 집중

---

## 4. 아키텍처 구조

계층 흐름 및 클래스 시그니처는 [`07-code-design.md`](07-code-design.md) 참고.
