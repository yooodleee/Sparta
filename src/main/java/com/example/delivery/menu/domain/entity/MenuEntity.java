package com.example.delivery.menu.domain.entity;

import com.example.delivery.global.infrastructure.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_product", indexes = {
        @Index(name = "idx_product_store", columnList = "storeId")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID storeId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Boolean isHidden = false;

    @Column(nullable = false)
    private  Boolean isSoldOut = false;

    @Column(length = 255)
    private String imageUrl;

    @Column(nullable = false)
    private Boolean aiDescription = false;

    private LocalDateTime deletedAt;
    private String deletedBy;

    @Builder
    public MenuEntity(UUID storeId, String name, String description, Integer price, Boolean isHidden, Boolean isSoldOut, String imageUrl, Boolean aiDescription){
        validatePrice(price);
        this.storeId = storeId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.isHidden = isHidden != null ? isHidden : false;
        this.isSoldOut = isSoldOut != null ? isSoldOut : false;
        this.imageUrl = imageUrl;
        this.aiDescription = aiDescription != null ? aiDescription : false;
    }

    public void updateProduct(String name, String description, Integer price, Boolean aiDescription, String imageUrl){
        if(price != null) validatePrice(price);
        if(name != null) this.name = name;
        if(description != null) this.description = description;
        if(price != null) this.price = price;
        if(aiDescription != null) this.aiDescription = aiDescription;
        if(imageUrl != null) this.imageUrl = imageUrl;
    }

    public void updateVisibility(boolean isHidden){
        this.isHidden = isHidden;
    }

    public void toggleSoldOut(){
        this.isSoldOut =!this.isSoldOut;
    }

    public void delete(String deletedBy){
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    private void validatePrice(Integer price){
        //DB 명세가 CHECK >=0 이므로, 0보다 작으면 에러 발생
        if(price != null && price <0){
            throw new IllegalArgumentException("상품 가격은 0원 이상이어야 합니다.");
        }
    }

    public void restore() {
        this.deletedAt = null;
        this.deletedBy = null;
        this.isHidden = true;
    }

}
