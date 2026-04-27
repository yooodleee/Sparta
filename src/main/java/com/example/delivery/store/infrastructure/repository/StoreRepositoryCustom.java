package com.example.delivery.store.infrastructure.repository;

import com.example.delivery.store.domain.entity.StoreEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Store 복합 검색 전용 Custom 레포지토리
 */
public interface StoreRepositoryCustom {

    Page<StoreEntity> search(String keyword, UUID categoryId, UUID areaId, Pageable pageable);
}
