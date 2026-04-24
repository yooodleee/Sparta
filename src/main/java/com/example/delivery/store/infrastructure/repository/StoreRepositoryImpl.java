package com.example.delivery.store.infrastructure.repository;

import com.example.delivery.store.domain.entity.StoreEntity;
import com.example.delivery.store.domain.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StoreRepositoryImpl implements StoreRepository {

    private final StoreJpaRepository storeJpaRepository;

    @Override
    public StoreEntity save(StoreEntity store) {
        return storeJpaRepository.save(store);
    }

    @Override
    public Optional<StoreEntity> findById(UUID storeId) {
        return storeJpaRepository.findByIdAndDeletedAtIsNull(storeId);
    }

    @Override
    public Optional<StoreEntity> findByOwnerIdAndName(UUID ownerId, String name) {
        return storeJpaRepository.findByOwnerIdAndNameAndDeletedAtIsNull(ownerId, name);
    }

    @Override
    public Page<StoreEntity> search(String keyword, UUID categoryId, UUID areaId, Pageable pageable) {
        return storeJpaRepository.search(keyword, categoryId, areaId, pageable);
    }

    @Override
    public boolean existsByCategoryId(UUID categoryId) {
        return storeJpaRepository.existsByCategoryIdAndDeletedAtIsNull(categoryId);
    }

    @Override
    public boolean existsByAreaId(UUID areaId) {
        return storeJpaRepository.existsByAreaIdAndDeletedAtIsNull(areaId);
    }
}
