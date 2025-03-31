package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.AccommodationReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.CustomReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.MyReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.ReviewRequest;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @Valid @RequestPart ReviewRequest reviewRequest,
      @RequestPart(required = false) List<MultipartFile> images) {

    reviewService.createReview(userDetails.getId(), reviewRequest, images);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @GetMapping("/users/reviews")
  public ResponseEntity<CustomReviewResponse<MyReviewResponse>> getUsersReviews(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @RequestParam(defaultValue = "0") Long cursor,
      @RequestParam(defaultValue = "20") @Range(min = 1, max = 100) int size) {

    return ResponseEntity.ok(reviewService.getUsersReviews(
        userDetails.getId(),
        cursor,
        size));
  }

  @GetMapping("/accommodations/{accommodationId}/reviews")
  public ResponseEntity<CustomReviewResponse<AccommodationReviewResponse>> getAccommodationReviews(
      @PathVariable Long accommodationId,
      @RequestParam(defaultValue = "0") Long cursor,
      @RequestParam(defaultValue = "20") @Range(min = 1, max = 100) int size) {

    return ResponseEntity.ok(reviewService.getAccommodationReviews(
        accommodationId, cursor, size));
  }

  @DeleteMapping("/users/reviews/{reviewId}")
  public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId,
      @AuthenticationPrincipal UserDetailsImpl userDetails) {

    reviewService.deleteReview(reviewId, userDetails.getId());

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
