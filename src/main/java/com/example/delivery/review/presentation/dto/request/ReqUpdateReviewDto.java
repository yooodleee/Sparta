package com.example.delivery.review.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReqUpdateReviewDto(
        @NotNull
        @Min(1) @Max(5)
        Integer rating,

        @Size(max = 200)
        String content
) {}