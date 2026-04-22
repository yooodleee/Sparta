package com.example.delivery.order.domain.repository;

import com.example.delivery.order.domain.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    OrderEntity save(OrderEntity order);

    Optional<OrderEntity> findById(UUID orderId);

    Page<OrderEntity> findAll(Pageable pageable);
}
