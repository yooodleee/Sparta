package com.example.delivery.menu.domain.repository;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//Repository 검색을 위한 도메인 레벨의 조건 객체
public record MenuSearchCondition(
    String keyword,
    BigDecimal minPrice,
    BigDecimal maxPrice,

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    LocalDateTime startDate,

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    LocalDateTime endDate,

    Boolean hidden
){
}
