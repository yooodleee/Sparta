package com.example.delivery.user.domain.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"Abcd1234!", "Qwer!234", "AaBb1!cd", "MaxPass1!1234xy"})
    void valid(String raw) {
        assertThatCode(() -> PasswordPolicy.validate(raw)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Abcd1!x",
            "Abcd1!23456789xy",
            "abcdefg1!",
            "ABCDEFG1!",
            "Abcdefgh!",
            "Abcdefgh1"
    })
    void invalid(String raw) {
        assertThatThrownBy(() -> PasswordPolicy.validate(raw))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
    }
}
