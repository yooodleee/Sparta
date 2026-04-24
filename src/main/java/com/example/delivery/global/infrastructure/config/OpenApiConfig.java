package com.example.delivery.global.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    private static final String API_DESCRIPTION = """
            배달 서비스 API 문서.

            ## 인증
            1. `POST /api/v1/auth/signup` 또는 `POST /api/v1/auth/login`으로 JWT 발급.
            2. 페이지 상단 **Authorize** 버튼 → `bearerAuth` 입력란에 토큰만 붙여넣기 (Bearer 접두사 불필요).
            3. 이후 보호 엔드포인트는 자동으로 `Authorization: Bearer <token>` 헤더가 부착.

            ## 공통 규약
            - 모든 응답은 `ApiResponse<T>` 래퍼 (`status`, `message`, `data`, `errors`) 포맷.
            - 권한 재검증: 매 요청 JWT payload role ↔ DB role 비교, 불일치 시 403.
            """;

    @Bean
    public OpenAPI deliveryOpenAPI() {
        SecurityScheme bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("`POST /api/v1/auth/login` 응답의 accessToken을 그대로 입력.");

        return new OpenAPI()
                .info(new Info()
                        .title("Delivery API")
                        .description(API_DESCRIPTION)
                        .version("v1"))
                .servers(List.of(new Server().url("http://localhost:8080").description("Local")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearer))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
