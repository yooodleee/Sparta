package com.example.delivery.store.application.service;

import com.example.delivery.area.domain.repository.AreaRepository;
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
public class StoreServiceV1 {

    private final StoreRepository storeRepository;
    private final AreaRepository areaRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public ResCreateStoreDto createStore(ReqCreateStoreDto request, UUID ownerId) {

        String storeName = request.name().trim();
        String address = request.address().trim();
        String phone = trimToNull(request.phone());

        validateCategoryExists(request.categoryId());
        validateAreaExists(request.areaId());
        validateDuplicateStoreName(ownerId, storeName);

        StoreEntity store = StoreEntity.builder()
                .ownerId(ownerId)
                .categoryId(request.categoryId())
                .areaId(request.areaId())
                .name(storeName)
                .address(address)
                .phone(phone)
                .build();

        return ResCreateStoreDto.from(storeRepository.save(store));
    }

    public PageResponse<ResGetStoreDto> getAllStores(String keyword, UUID categoryId, UUID areaId, int page, int size) {

        Pageable pageable = createPageable(page, size);
        String normalizedKeyword = hasKeyword(keyword) ? keyword.trim() : null;

        Page<ResGetStoreDto> result = storeRepository
                .search(normalizedKeyword, categoryId, areaId, pageable)
                .map(ResGetStoreDto::from);

        return PageResponse.from(result);
    }

    public ResGetStoreDto getStore(UUID storeId) {
        return ResGetStoreDto.from(getStoreEntity(storeId));
    }

    @Transactional
    public ResGetStoreDto updateStore(UUID storeId, ReqUpdateStoreDto request, UserPrincipal principal) {

        StoreEntity store = getStoreEntity(storeId);
        assertModifiable(store, principal);

        validateCategoryExists(request.categoryId());
        validateAreaExists(request.areaId());

        String newName = request.name().trim();
        // 이름이 바뀌는 경우에만 동일 소유자 내 중복 검증 (자기 자신은 제외)
        if (!store.getName().equals(newName)) {
            validateDuplicateStoreName(store.getOwnerId(), newName);
        }

        store.update(
                request.categoryId(),
                request.areaId(),
                newName,
                request.address().trim(),
                trimToNull(request.phone())
        );

        return ResGetStoreDto.from(store);
    }

    @Transactional
    public ResGetStoreDto toggleHiddenStore(UUID storeId, UserPrincipal principal) {

        StoreEntity store = getStoreEntity(storeId);
        assertModifiable(store, principal);

        store.toggleHidden();

        return ResGetStoreDto.from(store);
    }

    @Transactional
    public void deleteStore(UUID storeId, UserPrincipal principal) {

        StoreEntity store = getStoreEntity(storeId);
        assertDeletable(store, principal);

        store.softDelete(principal.username());
    }

    private StoreEntity getStoreEntity(UUID storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(StoreNotFoundException::new);
    }

    private void validateCategoryExists(UUID categoryId) {
        if (categoryRepository.findById(categoryId).isEmpty()) {
            throw new RelatedCategoryNotFoundException();
        }
    }

    private void validateAreaExists(UUID areaId) {
        if (areaRepository.findById(areaId).isEmpty()) {
            throw new RelatedAreaNotFoundException();
        }
    }

    private void validateDuplicateStoreName(UUID ownerId, String storeName) {
        if (storeRepository.findByOwnerIdAndName(ownerId, storeName).isPresent()) {
            throw new BusinessException(ErrorCode.STORE_ALREADY_EXISTS);
        }
    }

    /** PATCH (수정 / 숨김 토글) 권한: OWNER(본인) / MANAGER / MASTER */
    private void assertModifiable(StoreEntity store, UserPrincipal principal) {
        UserRole role = principal.role();
        if (role == UserRole.MANAGER || role == UserRole.MASTER) {
            return;
        }
        if (role == UserRole.OWNER && store.isOwnedBy(principal.id())) {
            return;
        }
        throw new StoreAccessDeniedException();
    }

    /** DELETE 권한: OWNER(본인) / MASTER (MANAGER 불가) */
    private void assertDeletable(StoreEntity store, UserPrincipal principal) {
        UserRole role = principal.role();
        if (role == UserRole.MASTER) {
            return;
        }
        if (role == UserRole.OWNER && store.isOwnedBy(principal.id())) {
            return;
        }
        throw new StoreAccessDeniedException();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
