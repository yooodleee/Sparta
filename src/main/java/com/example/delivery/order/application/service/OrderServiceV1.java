package com.example.delivery.order.application.service;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.order.domain.entity.OrderEntity;
import com.example.delivery.order.domain.entity.OrderItemEntity;
import com.example.delivery.order.domain.entity.OrderStatus;
import com.example.delivery.order.domain.repository.OrderRepository;
import com.example.delivery.order.presentation.dto.request.ReqCreateOrderDto;
import com.example.delivery.order.presentation.dto.request.ReqOrderItemDto;
import com.example.delivery.order.presentation.dto.response.ResOrderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceV1 {

    private final OrderRepository orderRepository;

    @Transactional
    public ResOrderDto createOrder(String customerId, ReqCreateOrderDto dto) {
        int totalPrice = dto.items().stream()
                .mapToInt(item -> item.quantity() * item.unitPrice())
                .sum();

        OrderEntity order = OrderEntity.builder()
                .customerId(customerId)
                .storeId(dto.storeId())
                .addressId(dto.addressId())
                .totalPrice(totalPrice)
                .request(dto.request())
                .build();

        for (ReqOrderItemDto item : dto.items()) {
            order.addItem(OrderItemEntity.builder()
                    .menuId(item.menuId())
                    .quantity(item.quantity())
                    .unitPrice(item.unitPrice())
                    .build());
        }

        return ResOrderDto.from(orderRepository.save(order));
    }

    public ResOrderDto getOrder(UUID orderId) {
        return ResOrderDto.from(findOrder(orderId));
    }

    public Page<ResOrderDto> getOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(ResOrderDto::from);
    }

    @Transactional
    public ResOrderDto cancelByCustomer(UUID orderId) {
        OrderEntity order = findOrder(orderId);
        order.cancelByCustomer(Clock.systemDefaultZone());
        return ResOrderDto.from(order);
    }

    @Transactional
    public ResOrderDto cancelByMaster(UUID orderId) {
        OrderEntity order = findOrder(orderId);
        order.cancelByMaster();
        return ResOrderDto.from(order);
    }

    @Transactional
    public ResOrderDto changeStatusByOwner(UUID orderId, OrderStatus next) {
        OrderEntity order = findOrder(orderId);
        order.changeStatusByOwner(next);
        return ResOrderDto.from(order);
    }

    @Transactional
    public ResOrderDto changeStatusByManager(UUID orderId, OrderStatus next) {
        OrderEntity order = findOrder(orderId);
        order.changeStatusByManager(next);
        return ResOrderDto.from(order);
    }

    @Transactional
    public ResOrderDto changeStatusByMaster(UUID orderId, OrderStatus next) {
        OrderEntity order = findOrder(orderId);
        order.changeStatusByMaster(next);
        return ResOrderDto.from(order);
    }

    @Transactional
    public ResOrderDto updateRequest(UUID orderId, String request) {
        OrderEntity order = findOrder(orderId);
        order.updateRequest(request);
        return ResOrderDto.from(order);
    }

    private OrderEntity findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }
}
