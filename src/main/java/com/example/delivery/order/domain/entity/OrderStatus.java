package com.example.delivery.order.domain.entity;

public enum OrderStatus {
    PENDING,
    ACCEPTED,
    COOKING,
    DELIVERING,
    DELIVERED,
    COMPLETED,
    CANCELLED;

    // 상태 흐름
    public boolean canTransitionTo(OrderStatus next){
        return switch (this){
            case PENDING -> next == ACCEPTED;
            case ACCEPTED -> next == COOKING;
            case COOKING -> next == DELIVERING;
            case DELIVERING -> next == DELIVERED;
            case DELIVERED -> next == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
