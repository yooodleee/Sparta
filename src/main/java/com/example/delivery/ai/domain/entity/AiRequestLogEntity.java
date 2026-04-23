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
    @Column(name = "ai_log_id")
    private UUID id;

    @Column(name = "user_id", length = 10, nullable = false)
    private String userId;

    //인증/인가 구현 후 User 매핑 예정 (현재는 BaseEntity의 createdBy로 추적)
    @Column(name = "request_text", length = 100, nullable = false)
    private String requestText;

    @Column(name = "response_text", columnDefinition = "TEXT")
    private String responseText;

    @Column(name = "request_type", length = 30, nullable = false)
    private String requestType;

    @Column(name = "menu_id")
    private UUID menuId;

    @Column(name = "is_applied", nullable = false)
    private Boolean isApplied = false;

    @Builder
    public AiRequestLogEntity(String userId, String requestText, String responseText, String requestType){
        this.userId = userId;
        this.requestText = requestText;
        this.responseText = responseText;
        this.requestType = requestType;
        this.isApplied = false;
    }

    public void assignToMenu(UUID menuId){
        this.menuId = menuId;
        this.isApplied = true;
    }
}
