package com.example.delivery.menu.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "메뉴(상품) 등록 요청 DTO")
public record ReqCreateMenuDto(

        @Schema(description = "상품 이름", example = "오리지널 치킨")
        @NotBlank(message = "상품 이름은 필수입니다.")
        @Size(max = 20, message = "상품 이름은 최대 20자까지 가능합니다.")
        String name,

        @Schema(description = "상품 설명", example = "바삭바삭하고 풍미 가득한 오리지널 치킨")
        @Size(max = 100, message = "상품 설명은 최대 100자까지 가능합니다.")
        String description,

        @Schema(description = "가격", example = "25000")
        @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
        Integer price,

        @Schema(description = "숨김 여부", example = "false")
        Boolean isHidden,

        @Schema(description = "품절 여부", example = "false")
        Boolean isSoldOut,

        @Schema(description = "이미지 URL", example = "https://s3.amaxon.com/.../")
        String imageUrl,

        @Schema(description = "AI 생성 시 발급받은 로그 ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID aiRequestId) {
}
