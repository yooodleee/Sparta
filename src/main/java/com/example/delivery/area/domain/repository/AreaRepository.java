package com.example.delivery.area.domain.repository;

import com.example.delivery.area.domain.entity.AreaEntity;

import java.util.Optional;

public interface AreaRepository {

    AreaEntity save(AreaEntity area);

    Optional<AreaEntity> findByNameIncludingDeleted(String areaName);

    Optional<AreaEntity> findByName(String areaName);
}
