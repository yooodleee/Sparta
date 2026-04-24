package com.example.delivery.order.domain.entity;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.global.infrastructure.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Getter
@Table(name = "p_order")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderEntity extends BaseEntity {

    private static final int CANCEL_LIMIT_MINUTES = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID orderId;

    @Column(name = "customer_id", nullable = false, length = 10)
    private String customerId;

    @Column(name = "store_id", nullable = false, columnDefinition = "UUID")
    private UUID storeId;

    @Column(name = "address_id", columnDefinition = "UUID")
    private UUID addressId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType = OrderType.ONLINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "request", columnDefinition = "TEXT")
    private String request;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items = new ArrayList<>();

    @Builder
    private OrderEntity(String customerId, UUID storeId, UUID addressId, Integer totalPrice, String request){
        this.customerId = customerId;
        this.storeId = storeId;
        this.addressId = addressId;
        this.totalPrice = totalPrice;
        this.request = request;
    }

    public void addItem(OrderItemEntity item){
        this.items.add(item);
        item.assignOrder(this);
    }

    public void cancelByCustomer(Clock clock){
        if(this.status != OrderStatus.PENDING){
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                    "PENDING 상태에서만 취소 가능합니다. (현재: %s)".formatted(this.status));
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if(Duration.between(getCreatedAt(), now).toMinutes() >= CANCEL_LIMIT_MINUTES){
            throw new BusinessException(ErrorCode.ORDER_CANCEL_TIMEOUT);
        }
        this.status = OrderStatus.CANCELLED;
        this.canceledAt = now;
    }

    public void cancelByMaster(){
        this.status = OrderStatus.CANCELLED;
        this.canceledAt = LocalDateTime.now();
    }

    public void changeStatusByOwner(OrderStatus next){
        if(!this.status.canTransitionTo(next)){
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                    "OWNER는 %s -> %s로 전이할 수 없습니다.".formatted(this.status, next));
        }
        this.status = next;
        stampTransition(next);
    }

    public void changeStatusByManager(OrderStatus next){
        if(next == OrderStatus.CANCELLED){
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "MANAGER는 주문을 취소할 수 없습니다. (현재: %s)".formatted(this.status));
        }
        this.status = next;
        stampTransition(next);
    }

    public void changeStatusByMaster(OrderStatus next){
        this.status = next;
        stampTransition(next);
    }

    private void stampTransition(OrderStatus next){
        LocalDateTime now = LocalDateTime.now();
        switch (next){
            case ACCEPTED -> this.acceptedAt = now;
            case DELIVERED -> this.deliveredAt = now;
            case CANCELLED -> this.canceledAt = now;
            default -> {}
        }
    }

    public void updateRequest(String request){
        if(this.status != OrderStatus.PENDING){
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                    "PENDING 상태에서만 요청사항을 수정할 수 있습니다. (현재: %s)".formatted(this.status));
        }
        this.request = request;
    }
}
