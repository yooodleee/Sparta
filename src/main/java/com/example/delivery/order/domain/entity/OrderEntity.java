package com.example.delivery.order.domain.entity;

import com.example.delivery.global.infrastructure.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Table(name = "p_order")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderEntity extends BaseEntity {

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
}
