package com.example.delivery.payment.domain.repository;

import com.example.delivery.payment.domain.entity.PaymentEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentRepository {

    PaymentEntity save(PaymentEntity payment);

    Optional<PaymentEntity> findById(UUID paymentId);

    Optional<PaymentEntity> findByOrderId(UUID orderId);

    Page<PaymentEntity> findAll(Pageable pageable);
}
