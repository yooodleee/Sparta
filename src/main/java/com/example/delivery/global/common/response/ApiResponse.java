package com.example.delivery.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        int status,
        String message,
        T data,
        List<FieldError> errors
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

    public record FieldError(String field, String message) {

    }
}
