package com.example.delivery.ai.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record ReqApplyAiDto(
        @Schema(description = "생성 단계에서 발급받은 로그 ID", example = "123a4567-b89c-10d1-e234-567891012000")
        UUID aiLogId,

        @Schema(
                description = "사장님이 수정한 메뉴 설명 (null인 경우 AI 원본 사용)",
                example = "사장님이 한 번 더 점검 후 수정한 단짠단짠 마늘 간장 치킨",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
            String description // owner가 수정한 메뉴 설명 (null이면 AI 원본 사용)
) { }
