package com.example.delivery.area.infrastructure.repository;

import com.example.delivery.area.domain.entity.AreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaJpaRepository extends JpaRepository<AreaEntity, Long> {
}
