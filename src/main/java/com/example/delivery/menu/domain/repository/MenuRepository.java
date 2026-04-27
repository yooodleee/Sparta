package com.example.delivery.menu.domain.repository;

import com.example.delivery.menu.domain.entity.MenuEntity;
import com.example.delivery.menu.presentation.dto.response.ResMenuDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuRepository {

    MenuEntity save(MenuEntity menu);

    Optional<MenuEntity> findById(UUID id);

    //특정 가게의 삭제/숨김 처리 안 된 메뉴 목록 조회
    List<MenuEntity> findVisibleMenusByStoreId(UUID storeId);

    //검색용 메서드
    List<MenuEntity> findVisibleMenusByStoreIdAndNameContaining(UUID storeId, String keyword);

    //조건 기반 검색 로직
    Page<ResMenuDto> findMenusByStoreCondition(UUID storeId, boolean isCustomer, MenuSearchCondition condition, Pageable pageable);
}
