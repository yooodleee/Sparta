package com.example.delivery.area.application.service;

import com.example.delivery.area.application.exception.AreaAlreadyDeletedException;
import com.example.delivery.area.application.exception.AreaAlreadyExistsException;
import com.example.delivery.area.domain.entity.AreaEntity;
import com.example.delivery.area.domain.repository.AreaRepository;
import com.example.delivery.area.presentation.dto.request.ReqCreateAreaDto;
import com.example.delivery.area.presentation.dto.response.ResCreateAreaDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AreaServiceV1 {

    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10, 30, 50);

    private final AreaRepository areaRepository;

    @Transactional
    public ResCreateAreaDto createArea(ReqCreateAreaDto request) {

        String areaName = request.name().trim();
        String city = request.city().trim();
        String district = request.district().trim();
        boolean isActive = request.isActive();

        validateDeletedArea(areaName);
        validateDuplicateAreaName(areaName);

        AreaEntity area = AreaEntity.builder()
                .name(areaName)
                .city(city)
                .district(district)
                .isActive(isActive)
                .build();

        return ResCreateAreaDto.from(areaRepository.save(area));
    }

    private void validateDeletedArea(String areaName) {
        if (areaRepository.findByNameIncludingDeleted(areaName).isPresent()) {
            throw new AreaAlreadyDeletedException();
        }

    }

    private void validateDuplicateAreaName(String areaName) {
        if (areaRepository.findByName(areaName).isPresent()) {
            throw new AreaAlreadyExistsException();
        }
    }
}
