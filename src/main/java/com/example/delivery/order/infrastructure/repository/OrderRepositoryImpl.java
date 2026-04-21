package com.example.delivery.order.infrastructure.repository;

import com.example.delivery.order.domain.entity.OrderEntity;
import com.example.delivery.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public OrderEntity save(OrderEntity order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<OrderEntity> findById(UUID orderId) {
        return orderJpaRepository.findById(orderId);
    }

    @Override
    public Page<OrderEntity> findAll(Pageable pageable) {
        return orderJpaRepository.findAll(pageable);
    }
}
