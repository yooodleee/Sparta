package com.example.delivery.global.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.List;

@Schema(description = "페이지네이션 응답")
public record PageResponse<T>(
        @Schema(description = "현재 페이지 콘텐츠") List<T> content,
        @Schema(description = "0-based 페이지 인덱스", example = "0") int page,
        @Schema(description = "페이지 크기 (10/30/50 중 하나)", example = "10") int size,
        @Schema(description = "전체 요소 수", example = "42") long totalElements,
        @Schema(description = "전체 페이지 수", example = "5") int totalPages,
        @Schema(description = "정렬 기준", example = "createdAt, DESC") String sort
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
