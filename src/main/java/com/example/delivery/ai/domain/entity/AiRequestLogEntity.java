package com.example.delivery.ai.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_ai_request_log",
        indexes = {
                @Index(name = "idx_ai_log_user_created", columnList = "userId, createdAt"),
                @Index(name = "idx_ai_log_menu_applid", columnList = "menuId, isApplied")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AiRequestLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ai_log_id")
    private UUID id;

    @Column(nullable = false, length = 10)
    private String userId;

    @Column(name = "request_text", nullable = false, length = 100)
    private String prompt;

    @Column(name = "response_text", columnDefinition = "TEXT")
    private String responseText;

    @Column(name = "request_type", nullable = false, length = 30)
    private String requestType;

    @Column(name = "menu_id")
    private UUID menuId;

    @Column(nullable = false)
    private Boolean isApplied = false;

    //BaseEntity 상속 대신 생성 관련 필드만 직접 선언 (수정/삭제 불가)
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(length = 10, updatable = false)
    private String createdBy;

    @Builder
    public AiRequestLogEntity(String userId, String prompt, String responseText, String requestType){
        this.userId = userId;
        this.prompt = prompt;
        this.responseText = responseText;
        this.requestType = requestType;
        this.isApplied = false;
    }

    public void assignToMenu(UUID menuId){
        this.menuId = menuId;
        this.isApplied = true;
    }
}
