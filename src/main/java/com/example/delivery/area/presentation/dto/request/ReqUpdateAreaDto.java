package com.example.delivery.area.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "지역 수정 요청 DTO")
public record ReqUpdateAreaDto(

        @Schema(description = "지역명", example = "강남")
        @NotBlank(message = "지역명은 필수입니다.")
        @Size(max = 100, message = "지역명은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "시/도", example = "서울특별시")
        @NotBlank(message = "시/도는 필수입니다.")
        @Size(max = 50, message = "시/도는 50자 이하여야 합니다.")
        String city,

        @Schema(description = "구/군", example = "강남구")
        @NotBlank(message = "구/군은 필수입니다.")
        @Size(max = 50, message = "구/군은 50자 이하여야 합니다.")
        String district,

        @Schema(description = "활성화 여부", example = "true")
        @NotNull(message = "활성화 여부는 필수입니다.")
        Boolean isActive
) {
}
