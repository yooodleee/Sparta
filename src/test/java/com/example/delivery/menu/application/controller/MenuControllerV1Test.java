package com.example.delivery.menu.application.controller;

import com.example.delivery.menu.application.service.MenuServiceV1;
import com.example.delivery.menu.presentation.controller.MenuControllerV1;
import com.example.delivery.menu.presentation.dto.request.ReqCreateMenuDto;
import com.example.delivery.menu.presentation.dto.request.ReqUpdateMenuDto;
import com.example.delivery.menu.presentation.dto.response.ResMenuDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MenuControllerV1.class)
public class MenuControllerV1Test {

    @Autowired
    private MockMvc mockMvc; //Postman 역할 가짜 클라이언트

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MenuServiceV1 menuService;

    @Test
    @DisplayName("POST /api/v1/stores/{storeId}/menus - 성공 시 201 응답")
    @WithMockUser
    void createMenu_api_success() throws Exception {

        UUID storeId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        ReqCreateMenuDto reqDto = new ReqCreateMenuDto("양념치킨", "K바베큐 양념을 곁들인 치킨", 28000, false, false, "https://image.dummy.com/chicken.jpg", UUID.randomUUID());

        //가짜 서비스가 반환할 응답 데이터
        ResMenuDto resDto = new ResMenuDto(menuId, storeId, "양념치킨", "k바베큐 양념을 곁들인 치킨", 28000, false, false, "https://image.dummy.com/chicken.jpg", true);
        given(menuService.createMenu(eq(storeId), any(ReqCreateMenuDto.class))).willReturn(resDto);

        //MockMvc로 실제 API 요청
        mockMvc.perform(post("/api/v1/stores/{storeId}/menus", storeId)
                        .with(csrf()) //가짜 CSRF
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDto))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("양념치킨"))
                .andExpect(jsonPath("$.data.price").value(28000))
                .andExpect(jsonPath("$.data.imageUrl").value("https://image.dummy.com/chicken.jpg"))
                .andExpect(jsonPath("$.data.aiDescription").value(true));

    }

    @Test
    @DisplayName("PATCH /api/v1/menus/{menuId} - 메뉴 수정")
    @WithMockUser
    void updateMenu_api_success() throws Exception {
        UUID menuId = UUID.randomUUID();
        ReqUpdateMenuDto reqDto = new ReqUpdateMenuDto("양념치킨", "매콤", 27000, null, null);
        ResMenuDto resDto = new ResMenuDto(menuId, UUID.randomUUID(), "양념치킨", "달콤", 27000, false, false, null, false);

        given(menuService.updateMenu(eq(menuId), any(ReqUpdateMenuDto.class))).willReturn(resDto);

        mockMvc.perform(patch("/api/v1/menus/{menuId}", menuId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("양념치킨"))
                .andExpect(jsonPath("$.data.price").value(27000));
    }

    @Test
    @DisplayName("DELETE /api/v1/menus/{menuId} - 메뉴 삭제")
    @WithMockUser
    void deleteMenu_api_success() throws Exception {
        UUID menuId = UUID.randomUUID();
        //삭제 성공했다고 가정
        mockMvc.perform(delete("/api/v1/menus/{menuId}", menuId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }


}
