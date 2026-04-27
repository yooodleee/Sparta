package com.example.delivery.ai.presentation.controller;

import com.example.delivery.ai.application.service.AiService;
import com.example.delivery.ai.domain.entity.AiRequestLogEntity;
import com.example.delivery.ai.presentation.dto.ReqApplyAiDto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/ai-description")
    public ResponseEntity<ResAiDescriptionDto> generatedAiDescription(@RequestBody AiRequestDto requestDto){
        //서비스 로직 호출
        AiRequestLogEntity logEntity = aiService.generatedAndLogDescription(requestDto.getMenuName());

        ResAiDescriptionDto responseDto = new ResAiDescriptionDto(
                logEntity.getId(),
                logEntity.getResponseText()
        );

        return ResponseEntity.ok(responseDto);
    }

    @PatchMapping("/{menuId}/ai-description/apply")
    public ResponseEntity<String> applyAiDescription(
            @PathVariable UUID menuId,
            @RequestBody ReqApplyAiDto request){

        aiService.applyAiDescription(menuId, request);

        return ResponseEntity.ok("AI 메뉴 설명이 적용되었습니다.");
    }


    @Getter
    public static class AiRequestDto{
        private String menuName;
    }

    public record ResAiDescriptionDto(
            UUID aiLogId,
            String responseText
    ){}
}
