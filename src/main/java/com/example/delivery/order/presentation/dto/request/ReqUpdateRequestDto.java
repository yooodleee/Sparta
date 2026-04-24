package com.example.delivery.order.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "주문 요청사항 수정 DTO")
public record ReqUpdateRequestDto(

        @Schema(description = "요청사항", example = "문 앞에 두고 가주세요.")
        @Size(max = 500, message = "요청사항은 최대 500자까지 가능합니다.")
        String request
) {
}
