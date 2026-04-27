package com.example.delivery.store.infrastructure.repository;

import com.example.delivery.store.domain.entity.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreJpaRepository extends JpaRepository<StoreEntity, UUID>, StoreRepositoryCustom {

    Optional<StoreEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<StoreEntity> findByOwnerIdAndNameAndDeletedAtIsNull(UUID ownerId, String name);

    boolean existsByCategoryIdAndDeletedAtIsNull(UUID categoryId);

    boolean existsByAreaIdAndDeletedAtIsNull(UUID areaId);
}
