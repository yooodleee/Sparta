package com.example.delivery.menu.infrastructure.repository;

import com.example.delivery.menu.domain.entity.MenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MenuJpaRepository extends JpaRepository<MenuEntity, UUID> {

    List<MenuEntity> findAllByStoreIdAndDeletedAtIsNullAndIsHiddenFalse(UUID storeId);
}
