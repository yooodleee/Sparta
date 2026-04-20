package com.example.delivery.review.presentation.controller;

import com.example.delivery.global.common.response.ApiResponse;
import com.example.delivery.global.infrastructure.security.UserPrincipal;
import com.example.delivery.review.application.service.ReviewService;
import com.example.delivery.review.presentation.dto.request.ReqCreateReviewDto;
import com.example.delivery.review.presentation.dto.request.ReqUpdateReviewDto;
import com.example.delivery.review.presentation.dto.response.ResReviewDto;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/orders/{orderId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResReviewDto> createReview(
            @PathVariable UUID orderId,
            @RequestBody @Valid ReqCreateReviewDto request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.created(reviewService.createReview(orderId, request, principal));
    }

    @GetMapping("/stores/{storeId}/reviews")
    public ApiResponse<Page<ResReviewDto>> getReviewsByStore(
            @PathVariable UUID storeId,
            @RequestParam(required = false) Integer rating,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.ok(reviewService.getReviewsByStore(storeId, rating, pageable));
    }

    @PatchMapping("/reviews/{reviewId}")
    public ApiResponse<ResReviewDto> updateReview(
            @PathVariable UUID reviewId,
            @RequestBody @Valid ReqUpdateReviewDto request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(reviewService.updateReview(reviewId, request, principal));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        reviewService.deleteReview(reviewId, principal);
    }
}