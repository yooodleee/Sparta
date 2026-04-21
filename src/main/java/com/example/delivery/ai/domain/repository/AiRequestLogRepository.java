package com.example.delivery.ai.domain.repository;

import com.example.delivery.ai.domain.entity.AiRequestLogEntity;

import java.util.Optional;
import java.util.UUID;

public interface AiRequestLogRepository {
    AiRequestLogEntity save(AiRequestLogEntity aiLog);
    Optional<AiRequestLogEntity> findById(UUID id);
}
