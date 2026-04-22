package com.example.delivery.area.application.service;

import com.example.delivery.area.application.exception.AreaAlreadyDeletedException;
import com.example.delivery.area.application.exception.AreaAlreadyExistsException;
import com.example.delivery.area.application.exception.AreaNotFoundException;
import com.example.delivery.area.domain.entity.AreaEntity;
import com.example.delivery.area.domain.repository.AreaRepository;
import com.example.delivery.area.presentation.dto.request.ReqCreateAreaDto;
import com.example.delivery.area.presentation.dto.response.ResCreateAreaDto;
import com.example.delivery.area.presentation.dto.response.ResGetAreaDto;
import com.example.delivery.global.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.example.delivery.global.common.pageable.PageableUtils.createPageable;
import static com.example.delivery.global.common.pageable.PageableUtils.hasKeyword;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AreaServiceV1 {

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

    public PageResponse<ResGetAreaDto> getAllAreas(String keyword, int page, int size) {

        Pageable pageable = createPageable(page, size);

        Page<ResGetAreaDto> result = hasKeyword(keyword)
                ? areaRepository.findByNameContaining(keyword.trim(), pageable)
                .map(ResGetAreaDto::from)
                : areaRepository.findAll(pageable)
                .map(ResGetAreaDto::from);

        return PageResponse.from(result);
    }

    public ResGetAreaDto getArea(UUID areaId) {
        return ResGetAreaDto.from(getAreaEntity(areaId));
    }

    private AreaEntity getAreaEntity(UUID areaId) {
        return areaRepository.findById(areaId)
                .orElseThrow(AreaNotFoundException::new);
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
