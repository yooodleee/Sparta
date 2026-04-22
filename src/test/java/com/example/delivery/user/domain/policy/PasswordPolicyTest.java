package com.example.delivery.user.domain.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"Abcd1234!", "Qwer!234", "AaBb1!cd", "MaxPass1!1234xy"})
    @DisplayName("대/소/숫/특수 포함 8~15자 허용")
    void valid(String raw) {
        assertThatCode(() -> PasswordPolicy.validate(raw)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Abcd1!x",             // 7자 (하한 경계 - 1)
            "Abcd1!23456789xy",    // 16자 (상한 경계 + 1)
            "abcdefg1!",           // 대문자 없음
            "ABCDEFG1!",           // 소문자 없음
            "Abcdefgh!",           // 숫자 없음
            "Abcdefgh1"            // 특수문자 없음
    })
    @DisplayName("규칙 위반은 INVALID_PASSWORD")
    void invalid(String raw) {
        assertThatThrownBy(() -> PasswordPolicy.validate(raw))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
    }

    @Test
    @DisplayName("null은 INVALID_PASSWORD")
    void nullValue() {
        assertThatThrownBy(() -> PasswordPolicy.validate(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
    }
}
