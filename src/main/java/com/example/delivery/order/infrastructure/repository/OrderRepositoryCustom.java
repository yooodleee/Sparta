package com.example.delivery.order.infrastructure.repository;

import com.example.delivery.order.domain.entity.OrderEntity;
import com.example.delivery.order.domain.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Order 복합 검색 전용 Custom 레포지토리.
 * customerId / storeId / status 조건을 선택적으로 조합해 QueryDSL 로 조회한다.
 */
public interface OrderRepositoryCustom {

    Page<OrderEntity> search(String customerId, UUID storeId, OrderStatus status, Pageable pageable);
}
