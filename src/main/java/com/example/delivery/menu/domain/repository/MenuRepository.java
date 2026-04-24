package com.example.delivery.menu.domain.repository;

import com.example.delivery.menu.domain.entity.MenuEntity;

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
}
