package com.example.delivery.store.infrastructure.repository;

import com.example.delivery.store.domain.entity.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreJpaRepository extends JpaRepository<StoreEntity, Long> {
}
