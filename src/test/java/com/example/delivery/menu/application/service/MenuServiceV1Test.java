package com.example.delivery.menu.application.service;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.menu.domain.entity.MenuEntity;
import com.example.delivery.menu.domain.repository.MenuRepository;
import com.example.delivery.menu.presentation.dto.request.ReqCreateMenuDto;
import com.example.delivery.menu.presentation.dto.request.ReqUpdateMenuDto;
import com.example.delivery.menu.presentation.dto.response.ResMenuDto;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class MenuServiceV1Test {

    @InjectMocks
    private MenuServiceV1 menuService;

    @Mock
    private MenuRepository menuRepository;

    @Test
    @DisplayName("메뉴 생성 성공 테스트")
    void createMenu_success() {
        UUID storeId = UUID.randomUUID();
        ReqCreateMenuDto reqDto = new ReqCreateMenuDto("오리지널치킨", "바삭바삭", 25000, false, false, null, null);

        MenuEntity savedMenu = MenuEntity.builder()
                .storeId(storeId)
                .name("오리지널치킨")
                .price(25000)
                .build();

        given(menuRepository.save(any(MenuEntity.class))).willReturn(savedMenu);
        ResMenuDto result = menuService.createMenu(storeId, reqDto);
        assertThat(result.name()).isEqualTo("오리지널치킨");
        assertThat(result.price()).isEqualTo(25000);
    }

    @Test
    @DisplayName("메뉴 단건 조회 실패 테스트 - 없는 메뉴 조회 시 BusinessException 발생")
    void getMenu_fail_notFound(){
        UUID fakeMenuId = UUID.randomUUID();
        given(menuRepository.findById(eq(fakeMenuId))).willReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> menuService.getMenu(fakeMenuId));
    }

    @Test
    @DisplayName("메뉴 수정 성공 테스트 - 일반 정보 수정 (AI 설명 없음)")
    void updateMenu_success(){
        UUID menuId = UUID.randomUUID();
        //기존 DB에 저장된 정보
        MenuEntity existingMenu = MenuEntity.builder()
                .name("오리지널 치킨")
                .price(25000)
                .build();

        //메뉴 이름과 가격 변경
        ReqUpdateMenuDto reqDto = new ReqUpdateMenuDto(
                "양념치킨", "매콤바삭", 27000, "http://new.jpg", null
        );

        given(menuRepository.findById(menuId)).willReturn(Optional.of(existingMenu));

        //실행
        ResMenuDto result = menuService.updateMenu(menuId, reqDto);

        //응답 결과 요청 반영 확인
        assertThat(result.name()).isEqualTo("양념치킨");
        assertThat(result.price()).isEqualTo(27000);
        assertThat(result.imageUrl()).isEqualTo("http://new.jpg");
    }

    @Test
    @DisplayName("메뉴 삭제 성공 테스트 (Soft Delete 검증)")
    void deleteMenu_success(){
        UUID menuId = UUID.randomUUID();
        String testUserId = "test";

        MenuEntity menu = MenuEntity.builder()
                .name("단종될 메뉴")
                .price(10000)
                .build();

        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));

        menuService.deleteMenu(menuId, testUserId);

        assertThat(menu.getDeletedAt()).isNotNull(); //삭제 시간
        assertThat(menu.getDeletedBy()).isEqualTo(testUserId); //삭제자 기록
    }

}
