package com.example.delivery.menu.application.service;

import com.example.delivery.ai.domain.entity.AiRequestLogEntity;
import com.example.delivery.ai.domain.repository.AiRequestLogRepository;
import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.menu.domain.entity.MenuEntity;
import com.example.delivery.menu.domain.repository.MenuRepository;
import com.example.delivery.menu.presentation.dto.request.ReqCreateMenuDto;
import com.example.delivery.menu.presentation.dto.response.ResMenuDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuServiceV1 {

    private final MenuRepository menuRepository;

    private final AiRequestLogRepository aiRequestLogRepository;

    @Transactional
    public ResMenuDto createMenu(UUID storeId, ReqCreateMenuDto reqDto) {

        //메뉴 생성

        //권한 검증 : Security 기능 완성되면 구현예정(현재 로그인한 유저가 이 storeId의 사장님이 맞는지 확인)

        MenuEntity newMenu = MenuEntity.builder()
                .storeId(storeId)
                .name(reqDto.name())
                .description(reqDto.description())
                .price(reqDto.price())
                .isHidden(reqDto.isHidden())
                .aiDescription(reqDto.aiRequestId() != null) //AI ID가 넘어왔다면 AI가 작성한 것으로 간주
                .build();

        MenuEntity savedMenu = menuRepository.save(newMenu);

        //AI 생성 요청이었다면, AI 로그 엔티티에 방금 생성된 메뉴의 ID(saveMenu.getId())를 매핑하고, isApplied = true 로 업데이트
        if (reqDto.aiRequestId() != null){
            AiRequestLogEntity aiLog = aiRequestLogRepository.findById(reqDto.aiRequestId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.AI_LOG_NOT_FOUND));

            aiLog.assignToMenu(savedMenu.getId());
        }

        return ResMenuDto.from(savedMenu);
    }

    //손님용 메뉴 목록 조회
    public List<ResMenuDto> getVisibleMenus(UUID storeId){
        //삭제 안 됨 + 숨김 안 됨 조건으로 리스트를 가져옴
        return menuRepository.findVisibleMenusByStoreId(storeId)
                .stream()
                .map(ResMenuDto::from)
                .toList();
    }

}
