package com.example.delivery.ai.presentation.controller;

import com.example.delivery.ai.application.service.AiServiceV1;
import com.example.delivery.ai.domain.entity.AiRequestLogEntity;
import com.example.delivery.ai.presentation.dto.ReqApplyAiDto;
import com.example.delivery.global.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
@Tag(name = "AI Menu API", description = "AI를 이용한 메뉴 설명 생성 및 관리 API")
public class AiControllerV1 {

    private final AiServiceV1 aiService;

    @PostMapping("/ai-description")
    @Operation(
            summary = "AI 메뉴 설명 생성 (임시 저장)",
            description = """ 
                    [기능 설명]<br> 
                    입력된 메뉴 이름을 바탕으로 Gemini AI가 설명을 생성하고 임시 로그를 저장합니다.<br><br>
                    [주의 사항]
                    1. 생성된 답변은 `p_ai_request_log`에 즉시 저장됩니다.
                    2. 응답으로 받은 aiLogId는 다음 단계인 '실제 반영(PATCH)' API 호출 시 필수입니다.
                    """
    )
    public ResponseEntity<ResAiDescriptionDto> generatedAiDescription(
            @RequestBody AiRequestDto requestDto,
            @AuthenticationPrincipal UserPrincipal userPrincipal){
        //서비스 로직 호출
        AiRequestLogEntity logEntity = aiService.generatedAndLogDescription(
                requestDto.menuName(),
                userPrincipal.getName()
        );

        ResAiDescriptionDto responseDto = new ResAiDescriptionDto(
                logEntity.getId(),
                logEntity.getResponseText()
        );

        return ResponseEntity.ok(responseDto);
    }

    @PatchMapping("/{menuId}/ai-description/apply")
    @Operation(
            summary = "AI 메뉴 설명 최종 적용",
            description = """
                [기능 설명]<br>
                선택한 AI 설명(또는 사용자가 수정한 설명)을 메뉴 설명에 반영합니다.<br><br>
                
                [데이터 반영 로직]
                1. 수정본 우선 반영 : `description`필드에 값이 들어오면, 사장님이 직접 수정한 것으로 판단하여 해당 내용을 메뉴에 반영합니다.
                2. 원본 자동 반영 : `description`필드가 null이거나 비어있으면, AI가 처음에 생성한 원본 텍스트를 그대로 반영합니다.<br><br>                 
                
                [흐름]<br>
                사장님이 글을 수정한 경우에만 `description`을 채워서 보내주면 됩니다.
                """
    )
    public ResponseEntity<String> applyAiDescription(
            @Parameter(description = "적용할 메뉴의 menuId")
            @PathVariable UUID menuId,
            @RequestBody ReqApplyAiDto request,
            @AuthenticationPrincipal UserPrincipal userPrincipal){

        aiService.applyAiDescription(menuId, request, userPrincipal.getName());

        return ResponseEntity.ok("AI 메뉴 설명이 적용되었습니다.");
    }

    @Schema(description = "AI 메뉴 설명을 생성하기 위한 요청 객체")
    public record AiRequestDto(
        @Schema(description = "AI 설명을 생성하고 싶은 메뉴의 이름", example = "마늘 간장 치킨")
        String menuName
    ){}

    public record ResAiDescriptionDto(
            UUID aiLogId,
            String description
    ){}
}
