package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.AccommodationReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.CustomReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.LatestReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.MyReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.ReviewContent;
import com.meongnyangerang.meongnyangerang.dto.ReviewRequest;
import com.meongnyangerang.meongnyangerang.dto.UpdateReviewRequest;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.ReviewService;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReviewController {

  private final ReviewService reviewService;

  // 리뷰 등록
  @PostMapping("/users/reviews")
  public ResponseEntity<Void> createReview(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @Valid @RequestPart ReviewRequest request,
      @RequestPart(required = false) List<MultipartFile> images) {

    reviewService.createReview(userDetails.getId(), request,
        (images != null) ? images : Collections.emptyList());

    return ResponseEntity.ok().build();
  }

  // 내 리뷰 조회
  @GetMapping("/users/reviews")
  public ResponseEntity<PageResponse<MyReviewResponse>> getUsersReviews(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @PageableDefault(size = 20, sort = "createdAt", direction = Direction.DESC) Pageable pageable) {

    return ResponseEntity.ok(reviewService.getUsersReviews(
        userDetails.getId(), pageable));
  }

  // 숙소 리뷰 목록 조회
  @GetMapping("/accommodations/{accommodationId}/reviews")
  public ResponseEntity<CustomReviewResponse<AccommodationReviewResponse>> getAccommodationReviews(
      @PathVariable Long accommodationId,
      @RequestParam(defaultValue = "0") Long cursor,
      @RequestParam(defaultValue = "20") @Range(min = 1, max = 100) int size) {

    return ResponseEntity.ok(reviewService.getAccommodationReviews(
        accommodationId, cursor, size));
  }

  // 내 리뷰 삭제
  @DeleteMapping("/users/reviews/{reviewId}")
  public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId,
      @AuthenticationPrincipal UserDetailsImpl userDetails) {

    reviewService.deleteReview(reviewId, userDetails.getId());

    return ResponseEntity.ok().build();
  }

  // 내 리뷰 수정
  @PutMapping("/users/reviews/{reviewId}")
  public ResponseEntity<Void> updateReview(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @PathVariable Long reviewId,
      @RequestPart(required = false) List<MultipartFile> newImages,
      @Valid @RequestPart UpdateReviewRequest request) {

    reviewService.updateReview(userDetails.getId(), reviewId,
        (newImages != null) ? newImages : Collections.emptyList(), request);

    return ResponseEntity.ok().build();
  }

  /**
   * 호스트의 숙소 리뷰 목록 조회
   */
  @GetMapping("/hosts/reviews")
  public ResponseEntity<PageResponse<ReviewContent>> getHostReviews(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @PageableDefault(size = 20, sort = "createdAt", direction = Direction.DESC)
      Pageable pageable
  ) {
    return ResponseEntity.ok(reviewService.getHostReviews(userDetails.getId(), pageable));
  }

  // 최신 리뷰 10개 조회
  @GetMapping("users/latest-reviews")
  public ResponseEntity<List<LatestReviewResponse>> getLatestReviews() {

    return ResponseEntity.ok(reviewService.getLatestReviews());
  }
}
