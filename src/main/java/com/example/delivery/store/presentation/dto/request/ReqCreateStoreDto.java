package com.example.delivery.store.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ReqCreateStoreDto(

        @NotNull(message = "카테고리 ID는 필수입니다.")
        UUID categoryId,

        @NotNull(message = "지역 ID는 필수입니다.")
        UUID areaId,

        @NotBlank(message = "가게명은 필수입니다.")
        @Size(max = 100, message = "가게명은 100자 이하여야 합니다.")
        String name,

        @NotBlank(message = "가게 주소는 필수입니다.")
        @Size(max = 255, message = "가게 주소는 255자 이하여야 합니다.")
        String address,

        @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
        @Pattern(
                regexp = "^$|^[0-9-]{9,20}$",
                message = "전화번호 형식이 올바르지 않습니다."
        )
        String phone
) {
}
