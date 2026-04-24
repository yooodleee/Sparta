package com.example.delivery.store.application.service;

import com.example.delivery.area.domain.entity.AreaEntity;
import com.example.delivery.area.domain.repository.AreaRepository;
import com.example.delivery.category.domain.entity.CategoryEntity;
import com.example.delivery.category.domain.repository.CategoryRepository;
import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.global.common.response.PageResponse;
import com.example.delivery.global.infrastructure.security.UserPrincipal;
import com.example.delivery.store.application.exception.RelatedAreaNotFoundException;
import com.example.delivery.store.application.exception.RelatedCategoryNotFoundException;
import com.example.delivery.store.application.exception.StoreAccessDeniedException;
import com.example.delivery.store.application.exception.StoreNotFoundException;
import com.example.delivery.store.domain.entity.StoreEntity;
import com.example.delivery.store.domain.repository.StoreRepository;
import com.example.delivery.store.presentation.dto.request.ReqCreateStoreDto;
import com.example.delivery.store.presentation.dto.request.ReqUpdateStoreDto;
import com.example.delivery.store.presentation.dto.response.ResCreateStoreDto;
import com.example.delivery.store.presentation.dto.response.ResGetStoreDto;
import com.example.delivery.user.domain.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
class StoreServiceV1Test {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private AreaRepository areaRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private StoreServiceV1 storeService;

    @Nested
    @DisplayName("가게 등록 테스트")
    class CreateStoreTest {

        @Test
        @DisplayName("가게 등록 성공")
        void createStore_success() throws Exception {
            // given
            UUID ownerId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            UUID areaId = UUID.randomUUID();

            ReqCreateStoreDto request = new ReqCreateStoreDto(
                    categoryId, areaId, "광화문 한식당", "서울 종로구 세종대로 1", "02-1234-5678"
            );

            StoreEntity saved = createStoreEntity(
                    ownerId, categoryId, areaId, "광화문 한식당", "서울 종로구 세종대로 1", "02-1234-5678"
            );

            given(categoryRepository.findById(categoryId)).willReturn(Optional.of(createCategoryEntity(categoryId)));
            given(areaRepository.findById(areaId)).willReturn(Optional.of(createAreaEntity(areaId)));
            given(storeRepository.findByOwnerIdAndName(ownerId, "광화문 한식당")).willReturn(Optional.empty());
            given(storeRepository.save(any(StoreEntity.class))).willReturn(saved);

            // when
            ResCreateStoreDto result = storeService.createStore(request, ownerId);

            // then
            assertThat(result.storeId()).isEqualTo(saved.getId());
            assertThat(result.ownerId()).isEqualTo(ownerId);
            assertThat(result.categoryId()).isEqualTo(categoryId);
            assertThat(result.areaId()).isEqualTo(areaId);
            assertThat(result.name()).isEqualTo("광화문 한식당");
            assertThat(result.isHidden()).isFalse();

            ArgumentCaptor<StoreEntity> captor = ArgumentCaptor.forClass(StoreEntity.class);
            verify(storeRepository).save(captor.capture());

            StoreEntity captured = captor.getValue();
            assertThat(captured.getOwnerId()).isEqualTo(ownerId);
            assertThat(captured.getName()).isEqualTo("광화문 한식당");
            assertThat(captured.getAddress()).isEqualTo("서울 종로구 세종대로 1");
            assertThat(captured.getPhone()).isEqualTo("02-1234-5678");
        }

        @Test
        @DisplayName("이름/주소 공백은 trim 되고 빈 phone 은 null 로 저장")
        void createStore_success_trimsAndNormalizesPhone() throws Exception {
            // given
            UUID ownerId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            UUID areaId = UUID.randomUUID();

            ReqCreateStoreDto request = new ReqCreateStoreDto(
                    categoryId, areaId, "  광화문 한식당  ", "  서울 종로구  ", "   "
            );

            StoreEntity saved = createStoreEntity(
                    ownerId, categoryId, areaId, "광화문 한식당", "서울 종로구", null
            );

            given(categoryRepository.findById(categoryId)).willReturn(Optional.of(createCategoryEntity(categoryId)));
            given(areaRepository.findById(areaId)).willReturn(Optional.of(createAreaEntity(areaId)));
            given(storeRepository.findByOwnerIdAndName(ownerId, "광화문 한식당")).willReturn(Optional.empty());
            given(storeRepository.save(any(StoreEntity.class))).willReturn(saved);

            // when
            storeService.createStore(request, ownerId);

            // then
            ArgumentCaptor<StoreEntity> captor = ArgumentCaptor.forClass(StoreEntity.class);
            verify(storeRepository).save(captor.capture());

            StoreEntity captured = captor.getValue();
            assertThat(captured.getName()).isEqualTo("광화문 한식당");
            assertThat(captured.getAddress()).isEqualTo("서울 종로구");
            assertThat(captured.getPhone()).isNull();
        }

        @Test
        @DisplayName("카테고리가 존재하지 않으면 실패")
        void createStore_fail_categoryNotFound() {
            // given
            UUID ownerId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            UUID areaId = UUID.randomUUID();

            ReqCreateStoreDto request = new ReqCreateStoreDto(
                    categoryId, areaId, "광화문 한식당", "서울", "02-1234-5678"
            );

            given(categoryRepository.findById(categoryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeService.createStore(request, ownerId))
                    .isInstanceOf(RelatedCategoryNotFoundException.class);

            verify(storeRepository, never()).save(any(StoreEntity.class));
        }

        @Test
        @DisplayName("지역이 존재하지 않으면 실패")
        void createStore_fail_areaNotFound() throws Exception {
            // given
            UUID ownerId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            UUID areaId = UUID.randomUUID();

            ReqCreateStoreDto request = new ReqCreateStoreDto(
                    categoryId, areaId, "광화문 한식당", "서울", "02-1234-5678"
            );

            given(categoryRepository.findById(categoryId)).willReturn(Optional.of(createCategoryEntity(categoryId)));
            given(areaRepository.findById(areaId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeService.createStore(request, ownerId))
                    .isInstanceOf(RelatedAreaNotFoundException.class);

            verify(storeRepository, never()).save(any(StoreEntity.class));
        }

        @Test
        @DisplayName("동일 소유자의 동일 이름 중복 시 실패")
        void createStore_fail_duplicateName() throws Exception {
            // given
            UUID ownerId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            UUID areaId = UUID.randomUUID();

            ReqCreateStoreDto request = new ReqCreateStoreDto(
                    categoryId, areaId, "광화문 한식당", "서울", "02-1234-5678"
            );

            StoreEntity existing = createStoreEntity(
                    ownerId, categoryId, areaId, "광화문 한식당", "서울", null
            );

            given(categoryRepository.findById(categoryId)).willReturn(Optional.of(createCategoryEntity(categoryId)));
            given(areaRepository.findById(areaId)).willReturn(Optional.of(createAreaEntity(areaId)));
            given(storeRepository.findByOwnerIdAndName(ownerId, "광화문 한식당")).willReturn(Optional.of(existing));

            // when & then
            assertThatThrownBy(() -> storeService.createStore(request, ownerId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STORE_ALREADY_EXISTS);

            verify(storeRepository, never()).save(any(StoreEntity.class));
        }
    }

    @Nested
    @DisplayName("가게 목록 조회 테스트")
    class GetAllStoresTest {

        @Test
        @DisplayName("필터 없이 전체 가게 목록 조회 성공")
        void getAllStores_success_noFilter() throws Exception {
            // given
            StoreEntity store1 = createStoreEntity(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "가게A", "서울", null
            );
            StoreEntity store2 = createStoreEntity(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "가게B", "서울", null
            );

            Page<StoreEntity> page = new PageImpl<>(
                    List.of(store1, store2),
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                    2
            );

            given(storeRepository.search(eq(null), eq(null), eq(null), any(Pageable.class))).willReturn(page);

            // when
            PageResponse<ResGetStoreDto> result = storeService.getAllStores(null, null, null, 0, 10);

            // then
            assertThat(result.content()).hasSize(2);
            assertThat(result.content().get(0).name()).isEqualTo("가게A");
            assertThat(result.content().get(1).name()).isEqualTo("가게B");
            assertThat(result.page()).isEqualTo(0);
            assertThat(result.size()).isEqualTo(10);
            assertThat(result.totalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("키워드 공백은 null 로 정규화되어 전체 조회")
        void getAllStores_blankKeyword_normalizedToNull() {
            // given
            Page<StoreEntity> empty = new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                    0
            );

            given(storeRepository.search(any(), any(), any(), any(Pageable.class))).willReturn(empty);

            // when
            storeService.getAllStores("   ", null, null, 0, 10);

            // then
            ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
            verify(storeRepository).search(keywordCaptor.capture(), eq(null), eq(null), any(Pageable.class));

            assertThat(keywordCaptor.getValue()).isNull();
        }

        @Test
        @DisplayName("키워드/카테고리/지역 필터 적용 조회 성공")
        void getAllStores_withAllFilters() throws Exception {
            // given
            UUID categoryId = UUID.randomUUID();
            UUID areaId = UUID.randomUUID();

            StoreEntity store = createStoreEntity(
                    UUID.randomUUID(), categoryId, areaId, "광화문 한식당", "서울", null
            );

            Page<StoreEntity> page = new PageImpl<>(
                    List.of(store),
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                    1
            );

            given(storeRepository.search(eq("광화문"), eq(categoryId), eq(areaId), any(Pageable.class))).willReturn(page);

            // when
            PageResponse<ResGetStoreDto> result = storeService.getAllStores(
                    "  광화문  ", categoryId, areaId, 0, 10
            );

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).name()).isEqualTo("광화문 한식당");

            ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
            verify(storeRepository).search(keywordCaptor.capture(), eq(categoryId), eq(areaId), any(Pageable.class));

            assertThat(keywordCaptor.getValue()).isEqualTo("광화문");
        }
    }

    @Nested
    @DisplayName("가게 상세 조회 테스트")
    class GetStoreTest {

        @Test
        @DisplayName("가게 상세 조회 성공")
        void getStore_success() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            StoreEntity store = createStoreEntity(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "광화문 한식당", "서울", "02-1234-5678"
            );
            setField(store, "id", storeId);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

            // when
            ResGetStoreDto result = storeService.getStore(storeId);

            // then
            assertThat(result.storeId()).isEqualTo(storeId);
            assertThat(result.name()).isEqualTo("광화문 한식당");
            assertThat(result.phone()).isEqualTo("02-1234-5678");
        }

        @Test
        @DisplayName("존재하지 않는 가게는 조회 실패")
        void getStore_fail_notFound() {
            // given
            UUID storeId = UUID.randomUUID();
            given(storeRepository.findById(storeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeService.getStore(storeId))
                    .isInstanceOf(StoreNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("가게 수정 테스트")
    class UpdateStoreTest {

        @Test
        @DisplayName("본인 OWNER 가 자신의 가게를 수정 성공")
        void updateStore_success_ownerSelf() throws Exception {
            // given
            UUID ownerId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            UUID areaId = UUID.randomUUID();

            StoreEntity store = createStoreEntity(
                    ownerId, categoryId, areaId, "광화문 한식당", "서울", "02-1111-1111"
            );
            setField(store, "id", storeId);

            ReqUpdateStoreDto request = new ReqUpdateStoreDto(
                    categoryId, areaId, "광화문 리뉴얼", "서울 종로구 2", "02-2222-2222"
            );
            UserPrincipal principal = new UserPrincipal(ownerId, "owner01", UserRole.OWNER);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
            given(categoryRepository.findById(categoryId)).willReturn(Optional.of(createCategoryEntity(categoryId)));
            given(areaRepository.findById(areaId)).willReturn(Optional.of(createAreaEntity(areaId)));
            given(storeRepository.findByOwnerIdAndName(ownerId, "광화문 리뉴얼")).willReturn(Optional.empty());

            // when
            ResGetStoreDto result = storeService.updateStore(storeId, request, principal);

            // then
            assertThat(result.name()).isEqualTo("광화문 리뉴얼");
            assertThat(store.getName()).isEqualTo("광화문 리뉴얼");
            assertThat(store.getAddress()).isEqualTo("서울 종로구 2");
            assertThat(store.getPhone()).isEqualTo("02-2222-2222");
        }

        @Test
        @DisplayName("MANAGER 는 타인 가게도 수정 가능")
        void updateStore_success_manager() throws Exception {
            // given
            UUID ownerId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            UUID areaId = UUID.randomUUID();

            StoreEntity store = createStoreEntity(
                    ownerId, categoryId, areaId, "원래이름", "서울", null
            );
            setField(store, "id", storeId);

            ReqUpdateStoreDto request = new ReqUpdateStoreDto(
                    categoryId, areaId, "새이름", "서울", null
            );
            UserPrincipal manager = new UserPrincipal(UUID.randomUUID(), "mng01", UserRole.MANAGER);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
            given(categoryRepository.findById(categoryId)).willReturn(Optional.of(createCategoryEntity(categoryId)));
            given(areaRepository.findById(areaId)).willReturn(Optional.of(createAreaEntity(areaId)));
            given(storeRepository.findByOwnerIdAndName(ownerId, "새이름")).willReturn(Optional.empty());

            // when
            ResGetStoreDto result = storeService.updateStore(storeId, request, manager);

            // then
            assertThat(result.name()).isEqualTo("새이름");
        }

        @Test
        @DisplayName("이름이 바뀌지 않은 경우 중복 검증은 생략")
        void updateStore_skipDuplicateCheck_whenNameUnchanged() throws Exception {
            // given
            UUID ownerId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            UUID areaId = UUID.randomUUID();

            StoreEntity store = createStoreEntity(
                    ownerId, categoryId, areaId, "광화문 한식당", "서울", null
            );
            setField(store, "id", storeId);

            ReqUpdateStoreDto request = new ReqUpdateStoreDto(
                    categoryId, areaId, "광화문 한식당", "서울 바뀐 주소", "02-9999-9999"
            );
            UserPrincipal principal = new UserPrincipal(ownerId, "owner01", UserRole.OWNER);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
            given(categoryRepository.findById(categoryId)).willReturn(Optional.of(createCategoryEntity(categoryId)));
            given(areaRepository.findById(areaId)).willReturn(Optional.of(createAreaEntity(areaId)));

            // when
            storeService.updateStore(storeId, request, principal);

            // then
            verify(storeRepository, never()).findByOwnerIdAndName(any(UUID.class), any(String.class));
            assertThat(store.getAddress()).isEqualTo("서울 바뀐 주소");
        }

        @Test
        @DisplayName("타인 OWNER 가 남의 가게 수정 시 실패")
        void updateStore_fail_otherOwner() throws Exception {
            // given
            UUID realOwnerId = UUID.randomUUID();
            UUID otherOwnerId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();

            StoreEntity store = createStoreEntity(
                    realOwnerId, UUID.randomUUID(), UUID.randomUUID(), "광화문 한식당", "서울", null
            );
            setField(store, "id", storeId);

            ReqUpdateStoreDto request = new ReqUpdateStoreDto(
                    UUID.randomUUID(), UUID.randomUUID(), "새이름", "서울", null
            );
            UserPrincipal other = new UserPrincipal(otherOwnerId, "other01", UserRole.OWNER);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() -> storeService.updateStore(storeId, request, other))
                    .isInstanceOf(StoreAccessDeniedException.class);

            verify(categoryRepository, never()).findById(any(UUID.class));
        }

        @Test
        @DisplayName("CUSTOMER 는 가게 수정 불가")
        void updateStore_fail_customer() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            StoreEntity store = createStoreEntity(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "광화문 한식당", "서울", null
            );
            setField(store, "id", storeId);

            ReqUpdateStoreDto request = new ReqUpdateStoreDto(
                    UUID.randomUUID(), UUID.randomUUID(), "새이름", "서울", null
            );
            UserPrincipal customer = new UserPrincipal(UUID.randomUUID(), "cust01", UserRole.CUSTOMER);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() -> storeService.updateStore(storeId, request, customer))
                    .isInstanceOf(StoreAccessDeniedException.class);
        }

        @Test
        @DisplayName("존재하지 않는 가게 수정 시 실패")
        void updateStore_fail_notFound() {
            // given
            UUID storeId = UUID.randomUUID();
            ReqUpdateStoreDto request = new ReqUpdateStoreDto(
                    UUID.randomUUID(), UUID.randomUUID(), "새이름", "서울", null
            );
            UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "owner01", UserRole.OWNER);

            given(storeRepository.findById(storeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeService.updateStore(storeId, request, principal))
                    .isInstanceOf(StoreNotFoundException.class);
        }

        @Test
        @DisplayName("이름 변경 시 동일 소유자 내 중복이면 실패")
        void updateStore_fail_duplicateNameAfterRename() throws Exception {
            // given
            UUID ownerId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            UUID areaId = UUID.randomUUID();

            StoreEntity store = createStoreEntity(
                    ownerId, categoryId, areaId, "원래이름", "서울", null
            );
            setField(store, "id", storeId);

            StoreEntity duplicated = createStoreEntity(
                    ownerId, categoryId, areaId, "새이름", "서울", null
            );

            ReqUpdateStoreDto request = new ReqUpdateStoreDto(
                    categoryId, areaId, "새이름", "서울", null
            );
            UserPrincipal principal = new UserPrincipal(ownerId, "owner01", UserRole.OWNER);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
            given(categoryRepository.findById(categoryId)).willReturn(Optional.of(createCategoryEntity(categoryId)));
            given(areaRepository.findById(areaId)).willReturn(Optional.of(createAreaEntity(areaId)));
            given(storeRepository.findByOwnerIdAndName(ownerId, "새이름")).willReturn(Optional.of(duplicated));

            // when & then
            assertThatThrownBy(() -> storeService.updateStore(storeId, request, principal))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STORE_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("가게 숨김 토글 테스트")
    class ToggleHiddenStoreTest {

        @Test
        @DisplayName("본인 OWNER 는 숨김 토글 가능")
        void toggleHiddenStore_success_ownerSelf() throws Exception {
            // given
            UUID ownerId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            StoreEntity store = createStoreEntity(
                    ownerId, UUID.randomUUID(), UUID.randomUUID(), "가게A", "서울", null
            );
            setField(store, "id", storeId);

            UserPrincipal owner = new UserPrincipal(ownerId, "owner01", UserRole.OWNER);
            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

            // when
            ResGetStoreDto result = storeService.toggleHiddenStore(storeId, owner);

            // then
            assertThat(result.isHidden()).isTrue();
            assertThat(store.isHidden()).isTrue();
        }

        @Test
        @DisplayName("MASTER 는 타인 가게도 숨김 토글 가능")
        void toggleHiddenStore_success_master() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            StoreEntity store = createStoreEntity(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "가게A", "서울", null
            );
            setField(store, "id", storeId);

            UserPrincipal master = new UserPrincipal(UUID.randomUUID(), "mst01", UserRole.MASTER);
            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

            // when
            storeService.toggleHiddenStore(storeId, master);

            // then
            assertThat(store.isHidden()).isTrue();
        }

        @Test
        @DisplayName("타인 OWNER 가 남의 가게 숨김 시도 시 실패")
        void toggleHiddenStore_fail_otherOwner() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            StoreEntity store = createStoreEntity(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "가게A", "서울", null
            );
            setField(store, "id", storeId);

            UserPrincipal other = new UserPrincipal(UUID.randomUUID(), "other01", UserRole.OWNER);
            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() -> storeService.toggleHiddenStore(storeId, other))
                    .isInstanceOf(StoreAccessDeniedException.class);

            assertThat(store.isHidden()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 가게 숨김 토글 시 실패")
        void toggleHiddenStore_fail_notFound() {
            // given
            UUID storeId = UUID.randomUUID();
            UserPrincipal master = new UserPrincipal(UUID.randomUUID(), "mst01", UserRole.MASTER);
            given(storeRepository.findById(storeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeService.toggleHiddenStore(storeId, master))
                    .isInstanceOf(StoreNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("가게 삭제 테스트")
    class DeleteStoreTest {

        @Test
        @DisplayName("본인 OWNER 는 자신의 가게 삭제 성공")
        void deleteStore_success_ownerSelf() throws Exception {
            // given
            UUID ownerId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            StoreEntity store = createStoreEntity(
                    ownerId, UUID.randomUUID(), UUID.randomUUID(), "가게A", "서울", null
            );
            setField(store, "id", storeId);

            UserPrincipal owner = new UserPrincipal(ownerId, "owner01", UserRole.OWNER);
            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

            // when
            storeService.deleteStore(storeId, owner);

            // then
            assertThat(store.isDeleted()).isTrue();
            assertThat(getField(store)).isEqualTo("owner01");
        }

        @Test
        @DisplayName("MASTER 는 타인 가게 삭제 성공")
        void deleteStore_success_master() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            StoreEntity store = createStoreEntity(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "가게A", "서울", null);
            setField(store, "id", storeId);

            UserPrincipal master = new UserPrincipal(UUID.randomUUID(), "mst01", UserRole.MASTER);
            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

            // when
            storeService.deleteStore(storeId, master);

            // then
            assertThat(store.isDeleted()).isTrue();
            assertThat(getField(store)).isEqualTo("mst01");
        }

        @Test
        @DisplayName("MANAGER 는 가게 삭제 불가")
        void deleteStore_fail_manager() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            StoreEntity store = createStoreEntity(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "가게A", "서울", null
            );
            setField(store, "id", storeId);

            UserPrincipal manager = new UserPrincipal(UUID.randomUUID(), "mng01", UserRole.MANAGER);
            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() -> storeService.deleteStore(storeId, manager))
                    .isInstanceOf(StoreAccessDeniedException.class);

            assertThat(store.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("타인 OWNER 는 남의 가게 삭제 불가")
        void deleteStore_fail_otherOwner() throws Exception {
            // given
            UUID storeId = UUID.randomUUID();
            StoreEntity store = createStoreEntity(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "가게A", "서울", null
            );
            setField(store, "id", storeId);

            UserPrincipal other = new UserPrincipal(UUID.randomUUID(), "other01", UserRole.OWNER);
            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() -> storeService.deleteStore(storeId, other))
                    .isInstanceOf(StoreAccessDeniedException.class);
        }

        @Test
        @DisplayName("존재하지 않는 가게 삭제 시 실패")
        void deleteStore_fail_notFound() {
            // given
            UUID storeId = UUID.randomUUID();
            UserPrincipal master = new UserPrincipal(UUID.randomUUID(), "mst01", UserRole.MASTER);
            given(storeRepository.findById(storeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeService.deleteStore(storeId, master))
                    .isInstanceOf(StoreNotFoundException.class);
        }
    }

    /**
     * Store 데이터 생성 메서드
     */
    private StoreEntity createStoreEntity(
            UUID ownerId, UUID categoryId, UUID areaId,
            String name, String address, String phone
    ) throws Exception {
        StoreEntity store = StoreEntity.builder()
                .ownerId(ownerId)
                .categoryId(categoryId)
                .areaId(areaId)
                .name(name)
                .address(address)
                .phone(phone)
                .build();

        setField(store, "id", UUID.randomUUID());
        setBaseEntityFields(
                store,
                LocalDateTime.of(2026, 1, 1, 12, 0),
                "owner01",
                LocalDateTime.of(2026, 1, 1, 12, 0),
                "owner01"
        );

        return store;
    }

    /**
     * Category 데이터 생성 메서드
     */
    private CategoryEntity createCategoryEntity(UUID categoryId) throws Exception {
        CategoryEntity category = CategoryEntity.builder()
                .name("한식")
                .build();

        setField(category, "id", categoryId);
        setBaseEntityFields(
                category,
                LocalDateTime.of(2026, 1, 1, 12, 0),
                "manager01",
                LocalDateTime.of(2026, 1, 1, 12, 0),
                "manager01"
        );

        return category;
    }

    /**
     * Area 데이터 생성 메서드
     */
    private AreaEntity createAreaEntity(UUID areaId) throws Exception {
        AreaEntity area = AreaEntity.builder()
                .name("광화문")
                .city("서울특별시")
                .district("종로구")
                .isActive(true)
                .build();

        setField(area, "id", areaId);
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
     * BaseEntity 기본 설정 메서드
     */
    private void setBaseEntityFields(
            Object target,
            LocalDateTime createdAt,
            String createdBy,
            LocalDateTime updatedAt,
            String updatedBy
    ) throws Exception {
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

    private Object getField(Object target) throws Exception {
        Class<?> clazz = target.getClass();

        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField("deletedBy");
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }

        throw new NoSuchFieldException("deletedBy");
    }
}
