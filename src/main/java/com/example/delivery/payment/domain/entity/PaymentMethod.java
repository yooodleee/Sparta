package com.example.delivery.payment.domain.entity;

/**
 * 결제 수단 Enum
 *
 * 현재는 CARD 단일 지원.
 * 향후 KAKAO_PAY, NAVER_PAY 등을 추가하더라도
 * 이 Enum에 값을 추가하는 것만으로 확장 가능.
 */
public enum PaymentMethod {
    CARD
}
