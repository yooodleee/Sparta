# 06. 인프라 명세

> 이전: [서비스 로직 명세](05-service-spec.md) · 다음: [코드 레벨 설계](07-code-design.md)

---

## 1. AWS 인프라 구성도 / 배포(CI/CD) 흐름

> **TBD — 추후 업데이트 예정.**
> AWS 서비스 아이콘 기반 인프라 구성도 및 CI/CD 파이프라인 다이어그램은 아래 항목이 확정된 뒤 추가한다.
> - 표현 수단 결정 (Mermaid `architecture-beta` / draw.io + AWS 스텐실 / 기타)
> - 실제 AWS 리소스 구성 확정 (단일 EC2 MVP 유지 여부, RDS 분리 시점 등)

## 2. 기술 스택

| 구분          | 기술                             | 비고                                  |
|-------------|--------------------------------|-------------------------------------|
| Language    | Java 17 (LTS)                  |                                     |
| Framework   | Spring Boot 3.x                | Web, Data JPA, Validation, Security |
| Security    | Spring Security + `jjwt`       | 매 요청 DB 권한 재검증                      |
| ORM         | JPA / Hibernate                |                                     |
| 복합 검색       | QueryDSL JPA                   | [도전]                                |
| DB          | PostgreSQL 15+                 | EC2에 직접 설치                          |
| Build       | Gradle 8.x                     |                                     |
| API 문서      | springdoc-openapi (Swagger UI) | `@Operation`/`@Schema` 인라인, RestDocs 미사용 |
| HTTP Client | `RestTemplate` 또는 `WebClient`  | Gemini 호출, 팀 선택                     |
| AI          | Gemini 1.5 Flash               | REST 호출                             |
| 로깅          | Logback                        | [도전]                                |
| Server      | AWS EC2 t2.micro               | Ubuntu 22.04                        |

## 3. 환경 프로필

| Profile | DB               | 용도          |
|---------|------------------|-------------|
| `local` | H2 In-Memory     | 개발자 개인 환경   |
| `test`  | H2 In-Memory     | JUnit 단위/통합 |
| `prod`  | PostgreSQL (EC2) | 운영 배포       |

- 프로필 전환: `-Dspring.profiles.active=prod`
- 환경별 `application-{profile}.yml` 분리

## 4. 네트워크 / 보안

- **Security Group 인바운드**: 22(SSH, 내 IP만), 8080(App, 0.0.0.0/0)
- **DB 포트 5432**: 외부 노출 X (로컬호스트 통신만)
- **민감 정보**: 환경 변수
    - `GEMINI_API_KEY`
    - `DB_PASSWORD`
    - `JWT_SECRET`
- **비밀번호**: BCrypt 해시 저장, 로그 출력 금지
- **JWT**: `Authorization: Bearer {token}`, HS256
- **권한 재검증**: JWT payload role ↔ DB role. 권한 변경 / 유저 삭제 시 기존 토큰 사용 불가
    - DB 부하 완화: 향후 Redis 캐시 도입 고려

## 5. 모니터링 / 로깅

- Logback로 `app.log` 파일 롤링 ([도전])
- 추후 고려: CloudWatch Agent 연동, Spring Actuator `/health`

## 6. 백업 / 복구 (MVP)

- 수동 `pg_dump` 주기 실행
- 운영 확장 시 RDS 전환 + 자동 스냅샷

## 7. 확장 로드맵

| 단계  | 변경                             |
|-----|--------------------------------|
| MVP | EC2 단일 노드 + PostgreSQL         |
| 확장1 | RDS 분리, S3 로그 적재               |
| 확장2 | Redis 캐시(권한/인기 쿼리)             |
| 확장3 | ALB + Auto Scaling, CloudFront |
