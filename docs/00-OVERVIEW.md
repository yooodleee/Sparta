# 배달 주문 관리 플랫폼 — 설계 문서 인덱스

**작성일**: 2026-04-16
**소스**: `요구사항_해설.md`
**아키텍처**: Monolithic · Layered (Controller → Service → Repository)
**기술 스택**: Java 17 · Spring Boot 3.x · Spring Security + JWT · JPA · PostgreSQL · Gradle

---

## 문서 구성

| #  | 문서                              | 내용                    |
|----|---------------------------------|-----------------------|
| 01 | [기능 명세](01-functional-spec.md)  | 기능 목록, 액터, 비기능 요구사항   |
| 02 | [도메인 명세](02-domain-spec.md)     | 도메인 모델, 상태 흐름, 권한 체계  |
| 03 | [데이터 명세](03-data-spec.md)       | ERD + 테이블 명세          |
| 04 | [API 명세](04-api-spec.md)        | REST 엔드포인트 요약 + 대표 예시 |
| 05 | [서비스 로직 명세](05-service-spec.md) | 주요 유스케이스 처리 흐름 (요약)   |
| 06 | [인프라 명세](06-infra-spec.md)      | 배포 아키텍처, 환경 프로필, 보안   |
| 07 | [코드 레벨 설계](07-code-design.md)   | 패키지 구조 + 클래스/서비스 스케치  |
| 08 | [애플리케이션 흐름](08-app-flow.md)    | 런타임 컴포넌트 도식 + 유스케이스별 시퀀스 다이어그램 |

### 팀 컨벤션 문서

| 문서                                          | 내용                                         |
|---------------------------------------------|--------------------------------------------|
| [팀 협업 및 기술 컨벤션](team-convention.md) (DRAFT) | PR/리뷰 프로세스, 문서화 체계, 아키텍처·JPA·DTO·예외·테스트 규칙 |
| [Git 컨벤션](git-convention.md)                | 브랜치/커밋/PR 네이밍, 머지 전략, 보호 규칙                |

## 핵심 규약 (요약)

- 모든 테이블 `p_` 접두사, PK는 **UUID v4** (유저 제외 — `username` VARCHAR PK)
- `BaseEntity` 상속: `created_at/by`, `updated_at/by`, `deleted_at/by` (일부 로그 테이블 제외)
- Soft Delete (`deleted_at`) 기본, 숨김(`is_hidden`)은 별개 필드
- 매 요청 시 JWT + DB 권한 재검증
- 주문 취소는 생성 후 5분 이내만 허용
- 결제는 CARD 단일, PG 미연동 (DB 저장만)
- AI 요청 텍스트 100자 제한, 응답 50자 이하 프롬프트 자동 삽입
- API 문서화: springdoc-openapi(Swagger UI) 사용 — `/swagger-ui/index.html`
- Service 단위 테스트는 **필수** (성공·실패 케이스), Controller 테스트는 권한·Validation 검증 중심, Repository 테스트는 미작성 (상세: [`team-convention.md`](team-convention.md) 3.5)

### 협업/Git 규약 (요약)

- 브랜치 네이밍: `[task]/[issue-no]-[summary]` (영문 소문자, 동사-목적어 2~4단어)
- 커밋/PR 제목: `[task]: [summary] (#[issue-no])`, 메시지 끝에 `(#이슈번호)` 필수
- Task 유형: `data` / `feature` / `bug` / `refactor` / `config` / `chores`
- `main` 직접 push 금지, 모든 변경은 PR을 통한 머지
- PR 단위: 작성자 판단에 맡기되, 리뷰어가 확인하기 불편하지 않은 수준으로 유지
- 머지 조건: 2명 Approve + CI 그린 + 컨플릭트 해소, 머지 실행은 PR 작성자 본인
- 머지 전략: **Squash 미사용**, 모든 커밋 이력이 남는 일반 Merge 방식
- CI: `./gradlew build test` 실행, 작성된 테스트의 통과 여부만 검증 (커버리지 기준 없음)
- 상세: [`git-convention.md`](git-convention.md), [`team-convention.md`](team-convention.md)
