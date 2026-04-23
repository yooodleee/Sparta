package com.example.delivery.ai.application.service;

import com.example.delivery.ai.domain.entity.AiRequestLogEntity;
import com.example.delivery.ai.domain.repository.AiRequestLogRepository;
import com.example.delivery.ai.infrastructure.client.GeminiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiService {

    private final GeminiClient geminiClient;
    private final AiRequestLogRepository aiRequestLogRepository;

    @Transactional
    public String generatedAndLogDescription(String menuName){

        String responseText = geminiClient.generateMenuDescription(menuName);

        AiRequestLogEntity logEntity = AiRequestLogEntity.builder()
                .userId("TEMP_USER") //Spring Security 연동 후 실제 유저 ID로 변경
                .requestText(menuName)
                .responseText(responseText)
                .requestType("PRODUCT_DESCTIPTION")
                .build();

        aiRequestLogRepository.save(logEntity);

        return responseText;
    }

}
