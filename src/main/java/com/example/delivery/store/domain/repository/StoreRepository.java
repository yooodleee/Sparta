package com.example.delivery.store.domain.repository;

import com.example.delivery.store.domain.entity.StoreEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface StoreRepository {

    StoreEntity save(StoreEntity store);

    Optional<StoreEntity> findById(UUID storeId);

    Optional<StoreEntity> findByOwnerIdAndName(UUID ownerId, String name);

    Page<StoreEntity> search(String keyword, UUID categoryId, UUID areaId, Pageable pageable);
}
