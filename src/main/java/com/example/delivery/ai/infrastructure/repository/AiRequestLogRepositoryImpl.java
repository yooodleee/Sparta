package com.example.delivery.ai.infrastructure.repository;

import com.example.delivery.ai.domain.entity.AiRequestLogEntity;
import com.example.delivery.ai.domain.repository.AiRequestLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AiRequestLogRepositoryImpl implements AiRequestLogRepository {

    private final AiRequestLogJpaRepository aiRequestLogJpaRepository;

    @Override
    public AiRequestLogEntity save(AiRequestLogEntity aiLog){
        return aiRequestLogJpaRepository.save(aiLog);
    }

    @Override
    public Optional<AiRequestLogEntity> findById(UUID id){
        return aiRequestLogJpaRepository.findById(id);
    }
}
