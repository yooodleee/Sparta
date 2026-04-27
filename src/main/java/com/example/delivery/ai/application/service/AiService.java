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
    public AiRequestLogEntity generatedAndLogDescription(String menuName, String userId){

        String responseText = geminiClient.generateMenuDescription(menuName);

        AiRequestLogEntity logEntity = AiRequestLogEntity.builder()
                .userId(userId)
                .requestText(menuName)
                .responseText(responseText)
                .requestType("PRODUCT_DESCRIPTION")
                .build();

         return aiRequestLogRepository.save(logEntity);

    }

    @Transactional
    public void applyAiDescription(UUID menuId, ReqApplyAiDto request, String userId){
        System.out.println("===========디버깅==========");
        System.out.println("넘어온 aiLogId: " + request.aiLogId());
        System.out.println("넘어온 수정본(description): " + request.description());
        System.out.println("==========================");

            AiRequestLogEntity aiLog = aiRequestLogRepository.findById(request.aiLogId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.AI_LOG_NOT_FOUND));

            MenuEntity menu = menuRepository.findById(menuId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

            if(aiLog.getIsApplied()){
                throw new BusinessException(ErrorCode.ALREADY_APPLIED_AI_LOG);
            }

            //owner 최종 수정 또는 AI가 생성한 설명 그대로 사용
            String finalDescription = (request.description() != null)
                                        ? request.description()
                                        : aiLog.getResponseText();


            menu.updateProduct(null, finalDescription, null, true, null);

            aiLog.assignToMenu(menuId);
        }
    }



