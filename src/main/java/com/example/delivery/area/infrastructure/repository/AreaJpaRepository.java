package com.example.delivery.area.infrastructure.repository;

import com.example.delivery.area.domain.entity.AreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AreaJpaRepository extends JpaRepository<AreaEntity, UUID> {

    Optional<AreaEntity> findByNameAndDeletedAtIsNotNull(String name);

    Optional<AreaEntity> findByName(String name);
}
