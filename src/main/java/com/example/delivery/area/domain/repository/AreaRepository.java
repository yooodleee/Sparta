package com.example.delivery.area.domain.repository;

import com.example.delivery.area.domain.entity.AreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaRepository extends JpaRepository<AreaEntity, Long> {
}
