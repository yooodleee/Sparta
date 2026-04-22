package com.example.delivery.user.domain.vo;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public Email {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL);
        }
    }
}
