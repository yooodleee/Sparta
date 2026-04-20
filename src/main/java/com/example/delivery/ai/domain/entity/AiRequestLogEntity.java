package com.example.delivery.ai.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_ai_request_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiRequestLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    //인증/인가 구현 후 User 매핑 예정 (현재는 BaseEntity의 createdBy로 추적)
    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "TEXT")
    private String responseText;

    private UUID menuId;

    @Column(nullable = false)
    private Boolean isApplied = false;

    @Builder
    public AiRequestLogEntity(String prompt, String responseText){
        this.prompt = prompt;
        this.responseText = responseText;
        this.isApplied = false;
    }

    public void assignToMenu(UUID menuId){
        this.menuId = menuId;
        this.isApplied = true;
    }
}
