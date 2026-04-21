package com.example.delivery.payment.infrastructure.repository;

import com.example.delivery.payment.domain.entity.PaymentEntity;
import com.example.delivery.payment.domain.repository.PaymentRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public PaymentEntity save(PaymentEntity payment) {
        return paymentJpaRepository.save(payment);
    }

    @Override
    public Optional<PaymentEntity> findById(UUID paymentId) {
        return paymentJpaRepository.findById(paymentId);
    }

    @Override
    public Optional<PaymentEntity> findByOrderId(UUID orderId) {
        return paymentJpaRepository.findByOrderId(orderId);
    }

    @Override
    public Page<PaymentEntity> findAll(Pageable pageable) {
        return paymentJpaRepository.findAll(pageable);
    }
}
