package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.ReviewReportRequest;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.ReviewReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReviewReportController {

  private final ReviewReportService reviewReportService;

  // 리뷰 신고
  @PostMapping("/reviews/{reviewId}/report")
  public ResponseEntity<Void> reportReview(
      @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long reviewId,
      @RequestPart @Valid ReviewReportRequest request,
      @RequestPart(required = false) MultipartFile evidenceImage) {

    reviewReportService.createReport(userDetails, reviewId, request, evidenceImage);

    return ResponseEntity.ok().build();
  }

}
