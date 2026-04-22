package com.example.delivery.area.infrastructure.repository;

import com.example.delivery.area.domain.entity.AreaEntity;
import com.example.delivery.area.domain.repository.AreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AreaRepositoryImpl implements AreaRepository {

    private final AreaJpaRepository areaJpaRepository;

    @Override
    public AreaEntity save(AreaEntity area) {
        return areaJpaRepository.save(area);
    }

    @Override
    public Optional<AreaEntity> findByNameIncludingDeleted(String areaName) {
        return areaJpaRepository.findByNameAndDeletedAtIsNotNull(areaName);
    }

    @Override
    public Optional<AreaEntity> findByName(String areaName) {
        return areaJpaRepository.findByName(areaName);
    }
}
