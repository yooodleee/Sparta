package com.example.delivery.user.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UsernameTest {

    @ParameterizedTest
    @ValueSource(strings = {"abcd", "abc123", "user00", "0123456789"})
    @DisplayName("소문자/숫자 4~10자는 유효")
    void valid(String raw) {
        assertThat(new Username(raw).value()).isEqualTo(raw);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "abcdefghijk", "Abcd", "ab_cd", "ab cd", ""})
    @DisplayName("길이/문자셋 위반은 INVALID_USERNAME")
    void invalid(String raw) {
        assertThatThrownBy(() -> new Username(raw))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_USERNAME);
    }

    @Test
    @DisplayName("null은 INVALID_USERNAME")
    void nullValue() {
        assertThatThrownBy(() -> new Username(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_USERNAME);
    }
}
