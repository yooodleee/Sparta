package com.example.delivery.review.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReqUpdateReviewDto(
        @NotNull(message = "평점은 필수 입력 항목입니다.")
        @Min(1) @Max(5)
        Integer rating,

        @Size(max = 200, message = "리뷰 내용은 200자 이내로 작성해주세요.")
        String content
) {}