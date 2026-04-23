package com.example.delivery.area.domain.repository;

import com.example.delivery.area.domain.entity.AreaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface AreaRepository {

    AreaEntity save(AreaEntity area);

    Optional<AreaEntity> findByNameIncludingDeleted(String areaName);

    Optional<AreaEntity> findByName(String areaName);

    Optional<AreaEntity> findById(UUID areaId);

    Page<AreaEntity> findAll(Pageable pageable);

    Page<AreaEntity> findByNameContaining(String keyword, Pageable pageable);
}
