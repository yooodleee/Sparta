package com.example.delivery.global.common.response;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sort
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                toSortString(page.getSort())
        );
    }

    private static String toSortString(Sort sort) {
        if (sort.isUnsorted()) {
            return "createdAt, DESC";
        }

        return sort.stream()
                .map(order -> order.getProperty() + ", " + order.getDirection().name())
                .reduce((a, b) -> a + ";" + b)
                .orElse("createdAt, DESC");
    }
}
