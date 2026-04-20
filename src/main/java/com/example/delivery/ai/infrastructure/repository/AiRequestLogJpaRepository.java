package com.example.delivery.ai.infrastructure.repository;

import com.example.delivery.ai.domain.entity.AiRequestLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiRequestLogJpaRepository extends JpaRepository<AiRequestLogEntity, UUID> {
}
