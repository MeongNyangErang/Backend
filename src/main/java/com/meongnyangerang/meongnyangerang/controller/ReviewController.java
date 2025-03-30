package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.AccommodationReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.CustomReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.MyReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.ReviewRequest;
import com.meongnyangerang.meongnyangerang.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

  @PostMapping("/users/reviews")
  public ResponseEntity<Void> createReview(
      //      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @Valid @RequestPart ReviewRequest reviewRequest,
      @RequestPart(required = false) List<MultipartFile> images) {

    // 로그인 기능 추가 되면 userDetails.getId()로 변경
    reviewService.createReview(1L, reviewRequest, images);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @GetMapping("/users/reviews")
  public ResponseEntity<CustomReviewResponse<MyReviewResponse>> getUsersReviews(
      //      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @RequestParam(defaultValue = "0") Long cursorId,
      @RequestParam(defaultValue = "20") @Range(min = 1, max = 100) int size) {

    CustomReviewResponse<MyReviewResponse> response = reviewService.getUsersReviews(1L, cursorId, size);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/accommodations/{accommodationId}/reviews")
  public ResponseEntity<CustomReviewResponse<AccommodationReviewResponse>> getAccommodationReviews(
      @PathVariable Long accommodationId,
      @RequestParam(defaultValue = "0") Long cursorId,
      @RequestParam(defaultValue = "20") @Range(min = 1, max = 100) int size) {

    CustomReviewResponse<AccommodationReviewResponse> response = reviewService.getAccommodationReviews(accommodationId, cursorId, size);

    return ResponseEntity.ok(response);
  }
}
