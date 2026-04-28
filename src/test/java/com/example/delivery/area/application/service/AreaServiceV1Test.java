package com.example.delivery.area.application.service;

import com.example.delivery.area.application.exception.AreaAlreadyDeletedException;
import com.example.delivery.area.application.exception.AreaAlreadyExistsException;
import com.example.delivery.area.application.exception.AreaNotFoundException;
import com.example.delivery.area.domain.entity.AreaEntity;
import com.example.delivery.area.domain.repository.AreaRepository;
import com.example.delivery.area.presentation.dto.request.ReqCreateAreaDto;
import com.example.delivery.area.presentation.dto.request.ReqUpdateAreaDto;
import com.example.delivery.area.presentation.dto.response.ResCreateAreaDto;
import com.example.delivery.area.presentation.dto.response.ResGetAreaDto;
import com.example.delivery.global.common.response.PageResponse;
import com.example.delivery.store.domain.repository.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AreaServiceV1Test {

    @Mock
    private AreaRepository areaRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private AreaServiceV1 areaService;

    @Nested
    @DisplayName("지역 생성 테스트")
    class CreateAreaTest {

        @Test
        @DisplayName("지역 생성 성공")
        void createArea_success() throws Exception {
            // given
            ReqCreateAreaDto request = new ReqCreateAreaDto(
                    "광화문",
                    "서울특별시",
                    "종로구",
                    true
            );

            AreaEntity saved = createAreaEntity("광화문", "서울특별시", "종로구", true);

            given(areaRepository.findByNameIncludingDeleted("광화문")).willReturn(Optional.empty());
            given(areaRepository.findByName("광화문")).willReturn(Optional.empty());
            given(areaRepository.save(any(AreaEntity.class))).willReturn(saved);

            // when
            ResCreateAreaDto result = areaService.createArea(request);

            // then
            assertThat(result.areaId()).isEqualTo(saved.getId());
            assertThat(result.name()).isEqualTo("광화문");
            assertThat(result.city()).isEqualTo("서울특별시");
            assertThat(result.district()).isEqualTo("종로구");
            assertThat(result.isActive()).isTrue();
            assertThat(result.createdBy()).isEqualTo("manager01");

            ArgumentCaptor<AreaEntity> captor = ArgumentCaptor.forClass(AreaEntity.class);
            verify(areaRepository).save(captor.capture());

            AreaEntity captorValue = captor.getValue();
            assertThat(captorValue.getName()).isEqualTo("광화문");
            assertThat(captorValue.getCity()).isEqualTo("서울특별시");
            assertThat(captorValue.getDistrict()).isEqualTo("종로구");
            assertThat(captorValue.isActive()).isTrue();
        }

        @Test
        @DisplayName("삭제된 지역 생성 시 실패")
        void createArea_fail_alreadyDeleted() throws Exception {
            // given
            AreaEntity deleted = createAreaEntity("광화문", "서울특별시", "종로구", true);
            setField(deleted, "deletedAt", LocalDateTime.now());

            ReqCreateAreaDto request = new ReqCreateAreaDto(
                    "광화문",
                    "서울특별시",
                    "종로구",
                    true
            );

            given(areaRepository.findByNameIncludingDeleted("광화문")).willReturn(Optional.of(deleted));

            // when & then
            assertThatThrownBy(() -> areaService.createArea(request))
                    .isInstanceOf(AreaAlreadyDeletedException.class);

            verify(areaRepository, never()).save(any(AreaEntity.class));
        }

        @Test
        @DisplayName("이미 존재하는 지역 생성 시 실패")
        void createArea_fail_duplicateName() throws Exception {
            // given
            ReqCreateAreaDto request = new ReqCreateAreaDto(
                    "광화문",
                    "서울특별시",
                    "종로구",
                    true
            );

            AreaEntity existed = createAreaEntity("광화문", "서울특별시", "종로구", true);

            given(areaRepository.findByNameIncludingDeleted("광화문")).willReturn(Optional.empty());
            given(areaRepository.findByName("광화문")).willReturn(Optional.of(existed));

            // when & then
            assertThatThrownBy(() -> areaService.createArea(request))
                    .isInstanceOf(AreaAlreadyExistsException.class);

            verify(areaRepository, never()).save(any(AreaEntity.class));
        }
    }

    @Nested
    @DisplayName("지역 목록 조회 테스트")
    class GetAllAreasTest {

        @Test
        @DisplayName("키워드 없이 전체 지역 목록 조회 성공")
        void getAllAreas_success_withoutKeyword() throws Exception {
            // given
            AreaEntity area1 = createAreaEntity("광화문", "서울특별시", "종로구", true);
            AreaEntity area2 = createAreaEntity("강남역", "서울특별시", "강남구", false);

            Page<AreaEntity> page = new PageImpl<>(
                    List.of(area1, area2),
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                    2
            );

            given(areaRepository.findAll(any(PageRequest.class))).willReturn(page);

            // when
            PageResponse<ResGetAreaDto> result = areaService.getAllAreas(null, 0, 10);

            // then
            assertThat(result.content()).hasSize(2);
            assertThat(result.content().get(0).name()).isEqualTo("광화문");
            assertThat(result.content().get(0).city()).isEqualTo("서울특별시");
            assertThat(result.content().get(0).district()).isEqualTo("종로구");
            assertThat(result.content().get(0).isActive()).isTrue();

            assertThat(result.content().get(1).name()).isEqualTo("강남역");
            assertThat(result.content().get(1).isActive()).isFalse();

            assertThat(result.page()).isEqualTo(0);
            assertThat(result.size()).isEqualTo(10);
            assertThat(result.totalPages()).isEqualTo(1);
            assertThat(result.totalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("키워드로 전체 지역 목록 조회 성공")
        void getAllAreas_success_withKeyword() throws Exception {
            // given
            AreaEntity area = createAreaEntity("광화문", "서울특별시", "종로구", true);

            Page<AreaEntity> page = new PageImpl<>(
                    List.of(area),
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                    1
            );

            given(areaRepository.findByNameContaining(eq("광"), any(Pageable.class))).willReturn(page);

            // when
            PageResponse<ResGetAreaDto> result = areaService.getAllAreas("광", 0, 10);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).name()).isEqualTo("광화문");
        }

        @Test
        @DisplayName("허용되지 않은 size는 10으로 보정")
        void getAllAreas_invalidSize_defaultToTen() throws Exception {
            // given
            Page<AreaEntity> empty = new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                    0
            );

            given(areaRepository.findAll(any(Pageable.class))).willReturn(empty);

            // when
            areaService.getAllAreas(null, 0, 7);

            // then
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(areaRepository).findAll(captor.capture());

            Pageable captorValue = captor.getValue();
            assertThat(captorValue.getPageNumber()).isEqualTo(0);
            assertThat(captorValue.getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("음수 page는 0으로 보정")
        void getAllAreas_invalidPage_defaultToZero() throws Exception {
            // given
            Page<AreaEntity> empty = new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                    0
            );

            given(areaRepository.findAll(any(Pageable.class))).willReturn(empty);

            // when
            areaService.getAllAreas(null, -1, 10);

            // then
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(areaRepository).findAll(captor.capture());

            Pageable captorValue = captor.getValue();
            assertThat(captorValue.getPageNumber()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("지역 상세 조회 테스트")
    class GetAreaTest {

        @Test
        @DisplayName("지역 상세 조회 성공")
        void getArea_success() throws Exception {
            // given
            UUID areaId = UUID.randomUUID();
            AreaEntity area = createAreaEntity("광화문", "서울특별시", "종로구", true);
            setField(area, "id", areaId);

            given(areaRepository.findById(areaId)).willReturn(Optional.of(area));

            // when
            ResGetAreaDto result = areaService.getArea(areaId);

            // then
            assertThat(result.areaId()).isEqualTo(areaId);
            assertThat(result.name()).isEqualTo("광화문");
            assertThat(result.city()).isEqualTo("서울특별시");
            assertThat(result.district()).isEqualTo("종로구");
            assertThat(result.isActive()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 지역일 시 상세 조회 실패")
        void getArea_fail_notFound() {
            // given
            UUID areaId = UUID.randomUUID();
            given(areaRepository.findById(areaId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> areaService.getArea(areaId))
                    .isInstanceOf(AreaNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("지역 수정 테스트")
    class UpdateAreaTest {

        @Test
        @DisplayName("지역 수정 성공")
        void updateArea_success() throws Exception {
            // given
            UUID areaId = UUID.randomUUID();
            AreaEntity area = createAreaEntity("광화문", "서울특별시", "종로구", true);
            setField(area, "id", areaId);

            ReqUpdateAreaDto request = new ReqUpdateAreaDto(
                    "강남역",
                    "서울특별시",
                    "강남구",
                    false
            );

            given(areaRepository.findById(areaId)).willReturn(Optional.of(area));
            given(areaRepository.findByName("강남역")).willReturn(Optional.empty());

            // when
            ResGetAreaDto result = areaService.updateArea(areaId, request);

            // then
            assertThat(result.name()).isEqualTo("강남역");
            assertThat(result.city()).isEqualTo("서울특별시");
            assertThat(result.district()).isEqualTo("강남구");
            assertThat(result.isActive()).isFalse();

            assertThat(area.getName()).isEqualTo("강남역");
            assertThat(area.getCity()).isEqualTo("서울특별시");
            assertThat(area.getDistrict()).isEqualTo("강남구");
            assertThat(area.isActive()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 지역이면 수정 실패")
        void updateArea_fail_notFound() {
            // given
            UUID areaId = UUID.randomUUID();
            ReqUpdateAreaDto request = new ReqUpdateAreaDto(
                    "강남역",
                    "서울특별시",
                    "강남구",
                    true
            );

            given(areaRepository.findById(areaId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> areaService.updateArea(areaId, request))
                    .isInstanceOf(AreaNotFoundException.class);
        }

        @Test
        @DisplayName("이미 존재하는 지역명일 시 수정 실패")
        void updateArea_fail_duplicateName() throws Exception {
            // given
            UUID areaId = UUID.randomUUID();
            AreaEntity area = createAreaEntity("광화문", "서울특별시", "종로구", true);
            AreaEntity existed = createAreaEntity("강남역", "서울특별시", "강남구", true);
            setField(area, "id", areaId);

            ReqUpdateAreaDto request = new ReqUpdateAreaDto(
                    "강남역",
                    "서울특별시",
                    "강남구",
                    true
            );

            given(areaRepository.findById(areaId)).willReturn(Optional.of(area));
            given(areaRepository.findByName("강남역")).willReturn(Optional.of(existed));

            // when & then
            assertThatThrownBy(() -> areaService.updateArea(areaId, request))
                    .isInstanceOf(AreaAlreadyExistsException.class);
        }
    }

    @Nested
    @DisplayName("지역 삭제 테스트")
    class deleteAreaTest {

        @Test
        @DisplayName("지역 삭제 성공")
        void deleteArea_success() throws Exception {
            // given
            UUID areaId = UUID.randomUUID();
            AreaEntity area = createAreaEntity("광화문", "서울특별시", "종로구", true);
            setField(area, "id", areaId);

            given(areaRepository.findById(areaId)).willReturn(Optional.of(area));

            // when
            areaService.deleteArea(areaId, "master01");

            // then
            assertThat(area.isDeleted()).isTrue();
            assertThat(getField(area, "deletedBy")).isEqualTo("master01");
            assertThat(getField(area, "deletedAt")).isNotNull();

            verify(storeRepository).existsByAreaId(areaId);
        }

        @Test
        @DisplayName("존재하지 않는 지역일 시 삭제 실패")
        void deleteArea_fail_notFound() {
            // given
            UUID areaId = UUID.randomUUID();
            given(areaRepository.findById(areaId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> areaService.deleteArea(areaId, "master01"))
                    .isInstanceOf(AreaNotFoundException.class);
        }
    }

    /**
     * Area 데이터 생성 메서드
     */
    private AreaEntity createAreaEntity(String name, String city, String district, boolean isActive) throws Exception {
        AreaEntity area = AreaEntity.builder()
                .name(name)
                .city(city)
                .district(district)
                .isActive(isActive)
                .build();

        setField(area, "id", UUID.randomUUID());
        setBaseEntityFields(
                area,
                LocalDateTime.of(2026, 1, 1, 12, 0),
                "manager01",
                LocalDateTime.of(2026, 1, 1, 12, 0),
                "manager01"
        );

        return area;
    }


    /**
     * Category Test 랑 중복
     */
    private void setBaseEntityFields(
            Object target,
            LocalDateTime createdAt,
            String createdBy,
            LocalDateTime updatedAt,
            String updatedBy
    ) throws Exception{
        setField(target, "createdAt", createdAt);
        setField(target, "createdBy", createdBy);
        setField(target, "updatedAt", updatedAt);
        setField(target, "updatedBy", updatedBy);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();

        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }

        throw new NoSuchFieldException(fieldName);
    }

    private Object getField(Object target, String fieldName) throws Exception {
        Class<?> clazz = target.getClass();

        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            }  catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
