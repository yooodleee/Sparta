package com.example.delivery.menu.application.integration;

import com.example.delivery.ai.application.service.AiServiceV1;
import com.example.delivery.ai.domain.entity.AiRequestLogEntity;
import com.example.delivery.ai.domain.repository.AiRequestLogRepository;
import com.example.delivery.ai.infrastructure.client.GeminiClient;
import com.example.delivery.ai.presentation.dto.ReqApplyAiDto;
import com.example.delivery.menu.domain.entity.MenuEntity;
import com.example.delivery.menu.domain.repository.MenuRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class MenuAiIntegrationTest{

    @Autowired
    private AiServiceV1 aiService;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private AiRequestLogRepository aiLogRepository;

    @MockBean
    private GeminiClient geminiClient;

    @Test
    @DisplayName("통합 테스트 - AI 메뉴 설명(2 Step)")
    void aiDescription_Precess_Integration(){
        //설명이 비어있는 신규 메뉴 생성
        UUID storeId = UUID.randomUUID();
        String userId = "ownerId";

        MenuEntity emptyMenu = MenuEntity.builder()
                .storeId(storeId)
                .name("눈꽃 치즈 떡볶이")
                .price(19000)
                .description(null)
                .build();

        MenuEntity savedMenu = menuRepository.save(emptyMenu);

        // STEP 1) AI 설명 생성 및 로그 적재(generatedAndLogDescription)

        //Gemini 가짜 응답 세팅
        String aiGeneratedText = "AI로 작성한 설명 : 눈꽃 치즈의 고소함이 풍부한 떡볶이";
        given(geminiClient.generateMenuDescription(anyString())).willReturn(aiGeneratedText);

        AiRequestLogEntity savedLog = aiService.generatedAndLogDescription(savedMenu.getName(), userId);

        // 검증 1: DB에 로그가 isApplied=true 반영됐는지
        AiRequestLogEntity dbLog = aiLogRepository.findById(savedLog.getId()).orElseThrow();
        assertThat(dbLog.getResponseText()).isEqualTo(aiGeneratedText);
        assertThat(dbLog.getIsApplied()).isFalse();

        // 이 시점에 실제 Menu의 description은 아직 바뀌면 안 됨
        assertThat(menuRepository.findById(savedMenu.getId()).orElseThrow().getDescription()).isNull();

        // STEP 2) 사장님 검토 후 반영(applyAiDescription)

        //사장님이 문구를 조금 다듬은 후 반영 요청
        String ownerModifiedText = "수정본 : 눈꽃 치즈가 사르르 녹아내리는 매콤달콤한 떡볶이";
        ReqApplyAiDto applyReq = new ReqApplyAiDto(dbLog.getId(), ownerModifiedText);

        aiService.applyAiDescription(savedMenu.getId(), applyReq, ownerModifiedText);

        // STEP 3) 최종 DB 무결성 교차 검증

        MenuEntity updatedMenu = menuRepository.findById(savedMenu.getId()).orElseThrow();
        AiRequestLogEntity appliedLog = aiLogRepository.findById(dbLog.getId()).orElseThrow();

        // 검증 2: 로그 상태가 true로 변경됐는지
        assertThat(appliedLog.getIsApplied()).isTrue();

        // 검증 3: 실제 메뉴 테이블에 사장님이 수정한 텍스트가 정확히 들어갔는지
        assertThat(updatedMenu.getDescription()).isEqualTo(ownerModifiedText);
    }
}