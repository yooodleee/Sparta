package com.example.delivery.user.domain.vo;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @ParameterizedTest
    @ValueSource(strings = {"a@b.co", "user.name+tag@example.com", "USER@domain.io"})
    @DisplayName("표준 형식 허용")
    void valid(String raw) {
        assertThat(new Email(raw).value()).isEqualTo(raw.toLowerCase(java.util.Locale.ROOT));
    }

    @Test
    @DisplayName("대소문자 혼합 이메일은 소문자로 정규화되어 저장")
    void normalizesToLowerCase() {
        Email email = new Email("User.Name@Example.COM");
        assertThat(email.value()).isEqualTo("user.name@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"plain", "a@", "@b.co", "a@b", "a b@c.co", ""})
    @DisplayName("형식 위반은 INVALID_EMAIL")
    void invalid(String raw) {
        assertThatThrownBy(() -> new Email(raw))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_EMAIL);
    }

    @Test
    @DisplayName("null은 INVALID_EMAIL")
    void nullValue() {
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_EMAIL);
    }
}
