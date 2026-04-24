package com.example.delivery.order.infrastructure.repository;

import com.example.delivery.order.domain.entity.OrderEntity;
import com.example.delivery.order.domain.entity.OrderStatus;
import com.example.delivery.order.domain.entity.QOrderEntity;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

    private static final QOrderEntity ORDER = QOrderEntity.orderEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<OrderEntity> search(String customerId, UUID storeId, OrderStatus status, Pageable pageable) {

        BooleanExpression[] conditions = new BooleanExpression[]{
                notDeleted(),
                customerIdEq(customerId),
                storeIdEq(storeId),
                statusEq(status)
        };

        List<OrderEntity> content = queryFactory
                .selectFrom(ORDER)
                .where(conditions)
                .orderBy(toOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(ORDER.count())
                .from(ORDER)
                .where(conditions)
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 조건 조립 (null 이면 조건 자체를 제외)
     */
    private BooleanExpression notDeleted() {
        return ORDER.deletedAt.isNull();
    }

    private BooleanExpression customerIdEq(String customerId) {
        return StringUtils.hasText(customerId) ? ORDER.customerId.eq(customerId) : null;
    }

    private BooleanExpression storeIdEq(UUID storeId) {
        return storeId == null ? null : ORDER.storeId.eq(storeId);
    }

    private BooleanExpression statusEq(OrderStatus status) {
        return status == null ? null : ORDER.status.eq(status);
    }

    /**
     * 정렬
     * Pageable 의 Sort 를 QueryDSL OrderSpecifier 로 변환
     * 지원 속성: createdAt, updatedAt
     * 지원하지 않는 속성은 무시하고, 결과가 비면 createdAt DESC 를 기본 적용
     */
    private OrderSpecifier<?>[] toOrderSpecifiers(Sort sort) {
        List<OrderSpecifier<?>> specifiers = new ArrayList<>();

        for (Sort.Order order : sort) {
            ComparableExpressionBase<?> path = resolvePath(order.getProperty());
            if (path == null) {
                continue;
            }
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            specifiers.add(new OrderSpecifier<>(direction, path));
        }

        if (specifiers.isEmpty()) {
            specifiers.add(ORDER.createdAt.desc());
        }

        return specifiers.toArray(new OrderSpecifier[0]);
    }

    private ComparableExpressionBase<?> resolvePath(String property) {
        return switch (property) {
            case "createdAt" -> ORDER.createdAt;
            case "updatedAt" -> ORDER.updatedAt;
            default -> null;
        };
    }
}
