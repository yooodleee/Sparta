package com.example.delivery.order.domain.entity;

public enum OrderStatus {
    PENDING,
    ACCEPTED,
    COOKING,
    DELIVERING,
    DELIVERED,
    COMPLETED,
    CANCELLED;

    /**
     * OWNER의 정방향 상태 전이만 검증합니다.
     * PENDING → ACCEPTED → COOKING → DELIVERING → DELIVERED → COMPLETED 로만 진행 가능하며,
     * CANCELLED 전이는 명세상 OWNER 권한이 아니므로 의도적으로 허용하지 않습니다.
     * (취소는 cancelByCustomer / cancelByMaster 경로에서만 처리)
     */
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
