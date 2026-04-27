package com.example.delivery.store.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.UUID;

@Schema(description = "가게 수정 요청 DTO")
public record ReqUpdateStoreDto(

        @Schema(description = "카테고리 ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull(message = "카테고리 ID는 필수입니다.")
        UUID categoryId,

        @Schema(description = "지역 ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull(message = "지역 ID는 필수입니다.")
        UUID areaId,

        @Schema(description = "가게명", example = "스파르타 분식")
        @NotBlank(message = "가게명은 필수입니다.")
        @Size(max = 100, message = "가게명은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "가게 주소", example = "서울특별시 강남구 테헤란로 1")
        @NotBlank(message = "가게 주소는 필수입니다.")
        @Size(max = 255, message = "가게 주소는 255자 이하여야 합니다.")
        String address,

        @Schema(description = "전화번호 (숫자/하이픈 허용, 9~20자)", example = "02-1234-5678")
        @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
        @Pattern(
                regexp = "^$|^[0-9-]{9,20}$",
                message = "전화번호 형식이 올바르지 않습니다."
        )
        String phone,

        @NotNull(message = "최소 주문 금액은 필수입니다.")
        @PositiveOrZero(message = "최소 주문 금액은 0원 이상이어야 합니다.")
        Integer minOrderAmount
) {
}
