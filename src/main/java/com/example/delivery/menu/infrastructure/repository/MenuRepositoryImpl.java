package com.example.delivery.menu.infrastructure.repository;


import com.example.delivery.menu.domain.entity.MenuEntity;
import com.example.delivery.menu.domain.repository.MenuRepository;
import com.example.delivery.menu.domain.repository.MenuSearchCondition;
import com.example.delivery.menu.presentation.dto.response.ResMenuDto;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.delivery.menu.domain.entity.QMenuEntity.menuEntity;
import static com.example.delivery.store.domain.entity.QStoreEntity.storeEntity;

@Repository
@RequiredArgsConstructor
public class MenuRepositoryImpl implements MenuRepository {

    private final MenuJpaRepository menuJpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public MenuEntity save(MenuEntity menu){
        return menuJpaRepository.save(menu);
    }

    @Override
    public Optional<MenuEntity> findById(UUID id){
        return menuJpaRepository.findById(id);
    }

    //조건 기반 검색 로직 추가
    @Override
    public Page<ResMenuDto> findMenusByStoreCondition(UUID storeId, boolean isCustomer, MenuSearchCondition condition, Pageable pageable){

        BooleanExpression[] conditions = buildConditions(storeId, isCustomer, condition);

        //데이터 조회
        List<ResMenuDto> content = queryFactory
                .select(Projections.constructor(ResMenuDto.class,
                        menuEntity.id,
                        menuEntity.storeId,
                        menuEntity.name,
                        menuEntity.description,
                        menuEntity.price,
                        menuEntity.isHidden,
                        menuEntity.isSoldOut,
                        menuEntity.imageUrl,
                        menuEntity.aiDescription
                ))
                .from(menuEntity)
                //N+1 문제 방지를 위한 Store 테이블 Join
                .join(storeEntity).on(menuEntity.storeId.eq(storeEntity.id))
                .where(conditions)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(toOrderSpecifiers(pageable.getSort()))
                .fetch();

        //카운트 (전체 개수)
        Long total = queryFactory
                .select(menuEntity.count())
                .from(menuEntity)
                .join(storeEntity).on(menuEntity.storeId.eq(storeEntity.id))
                .where(conditions)
                .fetchOne();

        return  new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private BooleanExpression[] buildConditions(UUID storeId, boolean isCustomer, MenuSearchCondition condition){
        return new BooleanExpression[]{
                menuEntity.storeId.eq(storeId), //해당 가게의 메뉴만

                //기본권한 필터링
                //고객일때만 Store의 상태(숨김, 삭제)를 필터링
                isCustomer ? storeEntity.deletedAt.isNull().and(storeEntity.isHidden.isFalse()) : null,
                //Menu 엔티티 자체에도 deletedAt/isHidden이 있다면 추가
                isCustomer ? menuEntity.deletedAt.isNull().and(menuEntity.isHidden.isFalse()) : null,

                //동적 검색 조건
                keywordContains(condition.keyword()),
                priceBetween(condition.minPrice(), condition.maxPrice()),
                createdAtBetween(condition.startDate(), condition.endDate()),
                hiddenEq(condition.hidden())
        };
    }

    private OrderSpecifier<?>[] toOrderSpecifiers(Sort sort){
        List<OrderSpecifier<?>> specifiers = new ArrayList<>();

        for(Sort.Order order : sort){
            ComparableExpressionBase<?> path = resolvePath(order.getProperty());
            if (path == null) continue;

            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            specifiers.add(new OrderSpecifier<>(direction, path));
        }

        //정렬 조건 없으면 기본값 생성일 역순(최신순) 적용
        if(specifiers.isEmpty()){
            specifiers.add(menuEntity.createdAt.desc());
        }

        return specifiers.toArray(new OrderSpecifier[0]);
    }

    private ComparableExpressionBase<?> resolvePath(String property){
        return switch (property){
            case "name" -> menuEntity.name;
            case "price" -> menuEntity.price;
            case "createdAt" -> menuEntity.createdAt;
            default -> null;
        };
    }

    private BooleanExpression keywordContains(String keyword){
        if(!StringUtils.hasText(keyword)) return null;
        return menuEntity.name.containsIgnoreCase(keyword)
                .or(menuEntity.description.containsIgnoreCase(keyword));
    }

    private BooleanExpression priceBetween(BigDecimal minPrice, BigDecimal maxPrice){
        if(minPrice == null && maxPrice == null) return null;
        if(minPrice != null && maxPrice == null) return menuEntity.price.goe(minPrice);
        if(minPrice == null) return menuEntity.price.loe(maxPrice);
        return menuEntity.price.between(minPrice, maxPrice);
    }

    private BooleanExpression createdAtBetween(LocalDateTime startDate, LocalDateTime endDate){
        if(startDate == null && endDate == null) return null;
        if(startDate != null && endDate == null) return menuEntity.createdAt.goe(startDate);
        if(startDate == null) return menuEntity.createdAt.loe(endDate);
        return menuEntity.createdAt.between(startDate, endDate);
    }

    private BooleanExpression hiddenEq(Boolean isHidden){
        return isHidden == null? null : menuEntity.isHidden.eq(isHidden);
    }

    @Override
    public List<MenuEntity> findVisibleMenusByStoreId(UUID storeId){
        return menuJpaRepository.findAllByStoreIdAndDeletedAtIsNullAndIsHiddenFalse(storeId);
    }

    @Override
    public List<MenuEntity> findVisibleMenusByStoreIdAndNameContaining(UUID storeId, String keyword){
        return menuJpaRepository.findAllByStoreIdAndNameContainingAndDeletedAtIsNullAndIsHiddenFalse(storeId, keyword);
    }
}
