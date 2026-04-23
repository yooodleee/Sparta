package com.example.delivery.ai.presentation;

import com.example.delivery.ai.application.service.AiService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/ai-description")
    public ResponseEntity<String> generatedAiDescription(@RequestBody AiRequestDto requestDto){
        //서비스 로직 호출
        String resultText = aiService.generatedAndLogDescription(requestDto.getMenuName());

        return ResponseEntity.ok(resultText);
    }

    @Getter
    public static class AiRequestDto{
        private String menuName;
    }
}
