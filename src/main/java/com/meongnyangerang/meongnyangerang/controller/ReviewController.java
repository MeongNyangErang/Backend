package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.ReviewRequest;
import com.meongnyangerang.meongnyangerang.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
