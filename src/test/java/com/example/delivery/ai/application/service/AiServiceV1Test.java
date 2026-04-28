package com.example.delivery.ai.application.service;

import org.springframework.test.util.ReflectionTestUtils;
import com.example.delivery.ai.domain.entity.AiRequestLogEntity;
import com.example.delivery.ai.domain.repository.AiRequestLogRepository;
import com.example.delivery.ai.infrastructure.client.GeminiClient;
import com.example.delivery.ai.presentation.dto.ReqApplyAiDto;
import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.menu.domain.entity.MenuEntity;
import com.example.delivery.menu.domain.repository.MenuRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class AiServiceV1Test {

    @InjectMocks
    private AiServiceV1 aiService;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private AiRequestLogRepository aiRequestLogRepository;

    @Mock
    private MenuRepository menuRepository;

    @Test
    @DisplayName("AI 설명 생성 및 로그 저장 성공")
    void generateAndLogDescription_Success(){
        String menuName = "불닭 치킨";
        String userId = "ownerId";
        String expectedAiResponse = "엄청 매운 불닭 소스를 입힌 치킨";

        given(geminiClient.generateMenuDescription(menuName)).willReturn(expectedAiResponse);

        AiRequestLogEntity savedLog = AiRequestLogEntity.builder()
                .userId(userId)
                .requestText(menuName)
                .responseText(expectedAiResponse)
                .requestType("PRODUCT_DESCRIPTION")
                .build();

        given(aiRequestLogRepository.save(any(AiRequestLogEntity.class))).willReturn(savedLog);

        AiRequestLogEntity result = aiService.generatedAndLogDescription(menuName, userId);

        assertThat(result.getResponseText()).isEqualTo(expectedAiResponse);
        assertThat(result.getRequestText()).isEqualTo(menuName);

    }

    @Test
    @DisplayName("AI 설명 메뉴에 반영 성공 - 수정본 적용")
    void applyAiDescription_Success() {
        UUID menuId = UUID.randomUUID();
        UUID aiLogId = UUID.randomUUID();
        String userId = "ownerId";
        ReqApplyAiDto reqDto = new ReqApplyAiDto(aiLogId, "수정한 메뉴 설명");

        AiRequestLogEntity aiLog = AiRequestLogEntity.builder()
                .responseText("AI로 설명을 쓴 치킨")
                .build();

        ReflectionTestUtils.setField(aiLog, "isApplied", false);

        MenuEntity menu = MenuEntity.builder().name("치킨").build();

        given(aiRequestLogRepository.findById(aiLogId)).willReturn(Optional.of(aiLog));
        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));

        aiService.applyAiDescription(menuId, reqDto, userId);
    }


        @Test
        @DisplayName("AI 설명 메뉴 반영 실패 - 이미 반영된 로그")
        void applyAiDescription_Fail_AlreadyApplied () {
            UUID menuId = UUID.randomUUID();
            UUID aiLogId = UUID.randomUUID();
            ReqApplyAiDto reqDto = new ReqApplyAiDto(aiLogId, "수정본");

            AiRequestLogEntity aiLog = AiRequestLogEntity.builder().build();
            ReflectionTestUtils.setField(aiLog, "isApplied", true);

            given(aiRequestLogRepository.findById(aiLogId)).willReturn(Optional.of(aiLog));
            given(menuRepository.findById(menuId)).willReturn(Optional.of(MenuEntity.builder().build()));

            assertThrows(BusinessException.class, () ->
                    aiService.applyAiDescription(menuId, reqDto, "ownerId")
            );
        }

}
