package com.example.delivery.store.infrastructure.repository;

import com.example.delivery.store.domain.entity.StoreEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StoreJpaRepository extends JpaRepository<StoreEntity, UUID> {

    Optional<StoreEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<StoreEntity> findByOwnerIdAndNameAndDeletedAtIsNull(UUID ownerId, String name);

    /**
     * 목록 검색: 삭제·숨김 제외 + 선택적 필터(keyword/categoryId/areaId).
     * 동적 조건은 파라미터 null 체크로 처리한다.
     * <p>
     * PostgreSQL + Hibernate 조합에서 null String 파라미터는 타입 정보가 유실되어
     * {@code bytea} 로 추정되는 이슈가 있으므로 {@code CAST(:keyword AS string)} 으로 명시한다.
     */
    @Query("""
            SELECT s FROM StoreEntity s
             WHERE s.deletedAt IS NULL
               AND s.isHidden = false
               AND (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
               AND (:categoryId IS NULL OR s.categoryId = :categoryId)
               AND (:areaId IS NULL OR s.areaId = :areaId)
            """)
    Page<StoreEntity> search(
            @Param("keyword") String keyword,
            @Param("categoryId") UUID categoryId,
            @Param("areaId") UUID areaId,
            Pageable pageable
    );
}
