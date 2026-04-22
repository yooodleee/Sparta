package com.example.delivery.user.domain.policy;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import java.util.regex.Pattern;

public final class PasswordPolicy {

    private static final Pattern PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>]).{8,15}$");

    private PasswordPolicy() {
    }

    public static void validate(String raw) {
        if (raw == null || !PATTERN.matcher(raw).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
    }
}
