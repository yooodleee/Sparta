package com.example.delivery.review.presentation.controller;

import com.example.delivery.global.common.response.ApiResponse;
import com.example.delivery.global.infrastructure.security.UserPrincipal;
import com.example.delivery.review.application.service.ReviewService;
import com.example.delivery.review.presentation.dto.request.ReqCreateReviewDto;
import com.example.delivery.review.presentation.dto.request.ReqUpdateReviewDto;
import com.example.delivery.review.presentation.dto.response.ResReviewDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Review", description = "리뷰 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 작성", description = "주문 완료 후 리뷰를 작성합니다. (인증 필요)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "리뷰 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 작성된 리뷰")
    })
    @PostMapping("/orders/{orderId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResReviewDto> createReview(
            @Parameter(description = "주문 ID") @PathVariable UUID orderId,
            @RequestBody @Valid ReqCreateReviewDto request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.created(reviewService.createReview(orderId, request, principal));
    }

    @Operation(summary = "가게 리뷰 목록 조회", description = "가게의 리뷰 목록을 페이지네이션으로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/stores/{storeId}/reviews")
    public ApiResponse<Page<ResReviewDto>> getReviewsByStore(
            @Parameter(description = "가게 ID") @PathVariable UUID storeId,
            @Parameter(description = "평점 필터 (1~5, 선택)") @RequestParam(required = false) Integer rating,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.ok(reviewService.getReviewsByStore(storeId, rating, pageable));
    }

    @Operation(summary = "리뷰 수정", description = "본인이 작성한 리뷰를 수정합니다. (인증 필요)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리뷰 없음")
    })
    @PatchMapping("/reviews/{reviewId}")
    public ApiResponse<ResReviewDto> updateReview(
            @Parameter(description = "리뷰 ID") @PathVariable UUID reviewId,
            @RequestBody @Valid ReqUpdateReviewDto request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(reviewService.updateReview(reviewId, request, principal));
    }

    @Operation(summary = "리뷰 삭제", description = "본인이 작성한 리뷰를 삭제합니다. 관리자(MANAGER/MASTER)도 삭제 가능합니다. (인증 필요)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리뷰 없음")
    })
    @DeleteMapping("/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(
            @Parameter(description = "리뷰 ID") @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        reviewService.deleteReview(reviewId, principal);
    }
}