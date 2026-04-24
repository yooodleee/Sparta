package com.example.delivery.user.domain.vo;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import java.util.regex.Pattern;

public record Username(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9]{4,10}$");

    public Username {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new BusinessException(ErrorCode.INVALID_USERNAME);
        }
    }
}
