package com.example.delivery.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "공통 응답 래퍼")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        @Schema(description = "HTTP 상태 코드", example = "200") int status,
        @Schema(description = "응답 메시지 또는 오류 코드", example = "SUCCESS") String message,
        @Schema(description = "실제 응답 데이터 (성공 시)") T data,
        @Schema(description = "필드 단위 검증 오류 목록 (400 VALIDATION_ERROR 일 때)") List<FieldError> errors
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "SUCCESS", data, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(201, "CREATED", data, null);
    }

    public static ApiResponse<Void> error(int status, String message) {
        return new ApiResponse<>(status, message, null, null);
    }

    public static ApiResponse<Void> validationError(String message, List<FieldError> errors) {
        return new ApiResponse<>(400, message, null, errors);
    }

    @Schema(description = "필드 단위 검증 오류")
    public record FieldError(
            @Schema(description = "오류가 발생한 필드명", example = "password") String field,
            @Schema(description = "필드별 오류 메시지", example = "size must be between 8 and 15") String message) {

    }
}
