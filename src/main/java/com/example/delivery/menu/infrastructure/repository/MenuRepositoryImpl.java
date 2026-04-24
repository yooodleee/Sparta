package com.example.delivery.menu.infrastructure.repository;

import com.example.delivery.menu.domain.entity.MenuEntity;
import com.example.delivery.menu.domain.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MenuRepositoryImpl implements MenuRepository {

    private final MenuJpaRepository menuJpaRepository;

    @Override
    public MenuEntity save(MenuEntity menu){
        return menuJpaRepository.save(menu);
    }

    @Override
    public Optional<MenuEntity> findById(UUID id){
        return menuJpaRepository.findById(id);
    }

    @Override
    public List<MenuEntity> findVisibleMenusByStoreId(UUID storeId){
        return menuJpaRepository.findAllByStoreIdAndDeletedAtIsNullAndIsHiddenFalse(storeId);
    }

    @Override
    public List<MenuEntity> findVisibleMenusByStoreIdAndNameContaining(UUID storeId, String keyword){
        return menuJpaRepository.findAllByStoreIdAndNameContainingAndDeletedAtIsNullAndIsHiddenFalse(storeId, keyword);
    }
}
