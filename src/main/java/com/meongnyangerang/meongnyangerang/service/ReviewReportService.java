package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.review.Review;
import com.meongnyangerang.meongnyangerang.domain.review.ReviewReport;
import com.meongnyangerang.meongnyangerang.dto.ReviewReportRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.ReviewReportRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewRepository;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ReviewReportService {

  private final ReviewReportRepository reviewReportRepository;
  private final ReviewRepository reviewRepository;
  private final ImageService imageService;

  // 리뷰 신고 생성
  @Transactional
  public void createReport(UserDetailsImpl userDetails, Long reviewId,
      ReviewReportRequest request, MultipartFile evidenceImage) {
    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.REVIEW_NOT_FOUND));

    boolean check = reviewReportRepository.existsByReporterIdAndReview(userDetails.getId(), review);

    if (check) {
      throw new MeongnyangerangException(ErrorCode.REVIEW_REPORT_ALREADY_EXISTS);
    }

    review.setReportCount(review.getReportCount() + 1);

    String storedImageUrl = null;
    if (evidenceImage != null && !evidenceImage.isEmpty()) {
      storedImageUrl = imageService.storeImage(evidenceImage);
    }

    ReviewReport report = request.toEntity(userDetails, review, storedImageUrl);

    reviewReportRepository.save(report);
  }
}
