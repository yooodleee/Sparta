package com.example.delivery.ai.application.service;

import com.example.delivery.ai.domain.entity.AiRequestLogEntity;
import com.example.delivery.ai.domain.repository.AiRequestLogRepository;
import com.example.delivery.ai.infrastructure.client.GeminiClient;
import com.example.delivery.ai.presentation.dto.ReqApplyAiDto;
import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.menu.domain.entity.MenuEntity;
import com.example.delivery.menu.domain.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiService {

    private final GeminiClient geminiClient;
    private final AiRequestLogRepository aiRequestLogRepository;
    private final MenuRepository menuRepository;

    @Transactional
    public AiRequestLogEntity generatedAndLogDescription(String menuName){

        String responseText = geminiClient.generateMenuDescription(menuName);

        AiRequestLogEntity logEntity = AiRequestLogEntity.builder()
                .userId("TEMP_USER") //Spring Security 연동 후 실제 유저 ID로 변경
                .requestText(menuName)
                .responseText(responseText)
                .requestType("PRODUCT_DESCRIPTION")
                .build();

         return aiRequestLogRepository.save(logEntity);

    }

    @Transactional
    public void applyAiDescription(UUID menuId, ReqApplyAiDto request){
            AiRequestLogEntity aiLog = aiRequestLogRepository.findById(request.aiLogId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.AI_LOG_NOT_FOUND));

            MenuEntity menu = menuRepository.findById(menuId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

            if(aiLog.getIsApplied()){
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "이미 적용된 AI 설명입니다.");
            }

            menu.updateProduct(null, aiLog.getResponseText(), null, true, null);

            aiLog.assignToMenu(menuId);
        }
    }



