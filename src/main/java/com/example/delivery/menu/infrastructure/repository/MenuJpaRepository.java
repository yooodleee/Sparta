package com.example.delivery.menu.infrastructure.repository;

import com.example.delivery.menu.domain.entity.MenuEntity;
import com.example.delivery.menu.presentation.dto.response.ResMenuDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.awt.*;
import java.util.List;
import java.util.UUID;

public interface MenuJpaRepository extends JpaRepository<MenuEntity, UUID> {

    List<MenuEntity> findAllByStoreIdAndDeletedAtIsNullAndIsHiddenFalse(UUID storeId);

    //이름으로 검색
    List<MenuEntity> findAllByStoreIdAndNameContainingAndDeletedAtIsNullAndIsHiddenFalse(UUID storeId, String keyword);


}
