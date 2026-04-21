package com.example.delivery.payment.infrastructure.repository;

import com.example.delivery.payment.domain.entity.PaymentEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByOrderId(UUID orderId);
}
