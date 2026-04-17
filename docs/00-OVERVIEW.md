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

## 핵심 규약 (요약)

- 모든 테이블 `p_` 접두사, PK는 UUID (유저 제외 — `username` VARCHAR PK)
- `BaseEntity` 상속: `created_at/by`, `updated_at/by`, `deleted_at/by` (일부 로그 테이블 제외)
- Soft Delete (`deleted_at`) 기본, 숨김(`is_hidden`)은 별개 필드
- 매 요청 시 JWT + DB 권한 재검증
- 주문 취소는 생성 후 5분 이내만 허용
- 결제는 CARD 단일, PG 미연동 (DB 저장만)
- AI 요청 텍스트 100자 제한, 응답 50자 이하 프롬프트 자동 삽입
- API 문서화: springdoc-openapi(Swagger UI) 사용 — `/swagger-ui/index.html`
- Repository/Service 단위 테스트는 **필수** (성공·실패 케이스 모두)
