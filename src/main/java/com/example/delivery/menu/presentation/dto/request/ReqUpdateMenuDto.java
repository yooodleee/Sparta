package com.example.delivery.menu.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "메뉴(상품) 수정 DTO")
public record ReqUpdateMenuDto(

        @Schema(description = "변경할 상품 이름")
        @Size(max = 20, message = "상품 이름은 최대 20자까지 가능합니다.")
        String name,

        @Schema(description = "변경할 상품 설명")
        @Size(max = 100, message = "상품 설명은 최대 100자까지 가능합니다.")
        String description,

        @Schema(description = "변경할 가격")
        @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
        Integer price,

        @Schema(description = "AI를 이용해 설명을 다시 생헝한 경우의 로그 ID")
        UUID aiRequestId) {
}
