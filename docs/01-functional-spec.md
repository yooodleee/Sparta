# 01. 기능 명세

> 상위: [OVERVIEW](00-OVERVIEW.md) · 다음: [도메인 명세](02-domain-spec.md)

---

## 1. 액터(Role)

| 코드         | 이름      | 주요 권한                                |
|------------|---------|--------------------------------------|
| `CUSTOMER` | 고객      | 주문 생성/조회, 리뷰 작성, 배송지 관리              |
| `OWNER`    | 가게 주인   | 본인 가게/메뉴/주문 관리, 주문 상태 변경             |
| `MANAGER`  | 서비스 담당자 | 모든 가게/주문/사용자 관리 (MANAGER 계정 CRUD 제외) |
| `MASTER`   | 최종 관리자  | MANAGER 포함 전체 권한                     |

## 2. 기능 목록 (FR)

### 2.1 인증 / 계정 (AUTH)

- **F-AUTH-01** 회원가입 (username 4~10자 소문자+숫자, password 8~15자 대소문자/숫자/특수문자 복합)
- **F-AUTH-02** 로그인 → JWT 발급 (Header 리턴)
- **F-AUTH-03** 매 요청 시 JWT payload의 role ↔ DB role 재검증
- **F-AUTH-04** 권한 변경 / 계정 삭제 시 기존 토큰 사용 불가

### 2.2 사용자 (USER)

- **F-USER-01** 사용자 목록/상세 조회 (권한별 범위 제한)
- **F-USER-02** 사용자 정보 수정 (본인: nickname/email/password/is_public)
- **F-USER-03** 사용자 권한 변경 (MASTER 전용, 별도 API)
- **F-USER-04** 사용자 Soft Delete

### 2.3 지역 / 카테고리 (AREA, CATEGORY)

- **F-AREA-01** 지역 CRUD + 목록 조회 (MANAGER/MASTER, 조회는 ALL)
- **F-CAT-01** 카테고리 CRUD + 목록 조회 (MANAGER/MASTER, 조회는 ALL)

### 2.4 가게 (STORE)

- **F-STORE-01** OWNER의 가게 등록
- **F-STORE-02** 가게 목록 조회 (keyword + categoryId + areaId 복합 필터 / [도전] QueryDSL)
- **F-STORE-03** 가게 수정/삭제 (본인 OWNER, MANAGER, MASTER)
- **F-STORE-04** 가게 숨김 처리 (`is_hidden`, 삭제와 별개)
- **F-STORE-05** 목록/상세 조회 시 `average_rating` 동시 노출 (N+1 방지)

### 2.5 메뉴 / 상품 (MENU)

- **F-MENU-01** 메뉴 등록 (OWNER, AI 설명 자동 생성 옵션)
- **F-MENU-02** 메뉴 조회/수정/삭제 + 숨김 처리
- **F-MENU-03** 메뉴 등록 시 `aiDescription=true`이면 Gemini 호출 → description 자동 채움 + 로그 기록
- **F-MENU-04** 삭제된 메뉴 복구(OWNER/MANAGER/MASTER, 안전을 위해 복구 시 `is_hidden = true` 자동 적용)

### 2.6 주문 (ORDER)

- **F-ORDER-01** 주문 생성 (CUSTOMER, 주문 시 단가 스냅샷 저장)
- **F-ORDER-02** 주문 요청사항 수정 (CUSTOMER, PENDING 상태만)
- **F-ORDER-03** 주문 상태 변경 (OWNER: 순서 준수 / MANAGER·MASTER: 자유)
- **F-ORDER-04** 주문 취소 (CUSTOMER 본인, 생성 후 5분 이내)
- **F-ORDER-05** 주문 목록/상세 조회 (본인·본인 가게 범위)

### 2.7 결제 (PAYMENT)

- **F-PAY-01** 주문에 대한 결제 생성 (CUSTOMER 본인, CARD만)
- **F-PAY-02** 결제 조회 (본인/MANAGER/MASTER)
- **F-PAY-03** 결제 상태 수정 (MANAGER, MASTER)

### 2.8 리뷰 (REVIEW)

- **F-REV-01** 리뷰 작성 (CUSTOMER, 주문 상태 COMPLETED, 1주문 1리뷰, 평점 1~5)
- **F-REV-02** 리뷰 조회 (storeId/rating 필터, 공개)
- **F-REV-03** 리뷰 수정/삭제 (본인 / MANAGER·MASTER 삭제)
- **F-REV-04** 리뷰 CUD 시 가게 `average_rating` 재집계 (동일 트랜잭션)

### 2.9 배송지 (ADDRESS)

- **F-ADDR-01** 배송지 등록/수정/삭제 (CUSTOMER 본인)
- **F-ADDR-02** 배송지 목록/상세 조회 (본인)
- **F-ADDR-03** 기본 배송지 설정 (한 사용자당 하나)

### 2.10 AI 연동 (AI)

- **F-AI-01** AI 상품 설명 독립 호출 엔드포인트 (OWNER)
- **F-AI-02** prompt 100자 제한 + 서버가 `"답변을 최대한 간결하게 50자 이하로"` 자동 삽입
- **F-AI-03** 모든 AI 요청/응답을 `p_ai_request_log`에 기록

## 3. 기능 우선순위

| 우선순위    | 기능                                                               |
|---------|------------------------------------------------------------------|
| P0 (필수) | AUTH, USER, STORE, MENU, ORDER, PAYMENT, ADDRESS, CATEGORY, AREA |
| P1 (필수) | REVIEW, AI 연동, 평균 평점 캐싱, 5분 취소 제한, Soft Delete                   |
| P2 (도전) | QueryDSL 복합 검색, Logback 로깅, AI 고도화(이미지/MCP)                      |

## 4. 비기능 요구사항 (NFR)

### 4.1 보안

- JWT Bearer, HS256. 매 요청 DB 권한 재검증 → 향후 Redis 캐싱 고려
- BCrypt 비밀번호 해시
- 민감 정보(Gemini API Key, DB 비밀번호)는 환경 변수로 주입
- Spring Validation으로 모든 Request DTO 검증

### 4.2 성능

- 페이지 사이즈 10/30/50만 허용 (그 외 기본 10)
- 기본 정렬 `createdAt DESC`
- 가게 평균 평점은 `average_rating` 컬럼 캐싱으로 N+1 회피

### 4.3 데이터

- 모든 테이블 `p_` 접두사
- PK는 UUID (유저만 `username` VARCHAR)
- Soft Delete 기본 (`deleted_at`), 숨김(`is_hidden`)은 별도 필드
- BaseEntity 상속으로 Audit 컬럼 자동 관리 (일부 로그 테이블 제외)

### 4.4 테스트

- Repository / Service 단위 테스트 **필수** (성공 + 실패 케이스)
- PR 머지 이전 테스트 통과가 전제 조건 (세부 전략·도구는 구현 단계에서 정의)

### 4.5 배포

- AWS EC2 t2.micro (Ubuntu 22.04) 단일 노드
- PostgreSQL은 EC2 직접 설치
- 프로필: `local`(H2) / `test`(H2) / `prod`(PostgreSQL)

### 4.6 API 문서화

- **springdoc-openapi(Swagger UI) 사용**. 실행 후 `/swagger-ui/index.html`에서 전체 API 확인 가능
- Controller/DTO에 `@Operation`, `@Schema` 등 어노테이션을 붙여 엔드포인트·요청/응답 명세를 자동 노출

## 5. 제약/가정

- 주문은 온라인만 가능 (`order_type=ONLINE`)
- 결제는 CARD만 허용, PG사 실제 연동 없음
- 초기 운영 지역: 광화문 한정 (모델은 전국 확장 가능)
- MASTER 계정은 시드로 생성
