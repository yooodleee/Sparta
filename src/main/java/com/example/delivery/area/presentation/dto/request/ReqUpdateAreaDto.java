package com.example.delivery.area.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReqUpdateAreaDto(

        @NotBlank(message = "지역명은 필수입니다.")
        @Size(max = 100, message = "지역명은 100자 이하여야 합니다.")
        String name,

        @NotBlank(message = "시/도는 필수입니다.")
        @Size(max = 50, message = "시/도는 50자 이하여야 합니다.")
        String city,

        @NotBlank(message = "구/군은 필수입니다.")
        @Size(max = 50, message = "구/군은 50자 이하여야 합니다.")
        String district,

        @NotNull(message = "활성화 여부는 필수입니다.")
        Boolean isActive
) {
}
