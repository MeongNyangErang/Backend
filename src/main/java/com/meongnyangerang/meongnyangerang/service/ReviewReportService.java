package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.review.ReportStatus;
import com.meongnyangerang.meongnyangerang.domain.review.ReporterType;
import com.meongnyangerang.meongnyangerang.domain.review.Review;
import com.meongnyangerang.meongnyangerang.domain.review.ReviewReport;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.ReviewReportDetailResponse;
import com.meongnyangerang.meongnyangerang.dto.ReviewReportRequest;
import com.meongnyangerang.meongnyangerang.dto.ReviewReportResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewReportRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ReviewReportService {

  private final ReviewReportRepository reviewReportRepository;
  private final ReviewRepository reviewRepository;
  private final UserRepository userRepository;
  private final HostRepository hostRepository;
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

  public PageResponse<ReviewReportResponse> getReviews(Pageable pageable) {
    Page<ReviewReport> reviewReportResponse = reviewReportRepository.findAllByStatus(
        pageable, ReportStatus.PENDING);
    Page<ReviewReportResponse> responses = reviewReportResponse.map(ReviewReportResponse::from);
    return PageResponse.from(responses);
  }

  public ReviewReportDetailResponse getReviewReportDetail(Long reviewReportId) {
    ReviewReport reviewReport = reviewReportRepository.findById(reviewReportId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.NOT_EXIST_REVIEW_REPORT));

    String reporterNickname = getReporterNickname(reviewReport.getReporterId(),
        reviewReport.getType());

    return ReviewReportDetailResponse.from(reviewReport, reporterNickname);
  }

  @Transactional
  public void processReviewReport(Long reviewReportId) {
    ReviewReport reviewReport = reviewReportRepository.findById(reviewReportId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.NOT_EXIST_REVIEW_REPORT));

    Review review = reviewReport.getReview();
    review.setHidden(true);
    review.setHiddenAt(LocalDateTime.now());

    // 해당 리뷰에 달린 모든 신고 상태를 COMPLETED 로 처리
    List<ReviewReport> allReports = reviewReportRepository.findAllByReviewId(review.getId());
    for (ReviewReport report : allReports) {
      report.setStatus(ReportStatus.COMPLETED);
    }
  }

  private String getReporterNickname(Long reporterId, ReporterType type) {
    if (type == ReporterType.USER) {
      return userRepository.findById(reporterId)
          .map(User::getNickname)
          .orElseThrow(() -> new MeongnyangerangException(ErrorCode.USER_NOT_FOUND));
    } else if (type == ReporterType.HOST) {
      return hostRepository.findById(reporterId)
          .map(Host::getNickname)
          .orElseThrow(() -> new MeongnyangerangException(ErrorCode.NOT_EXISTS_HOST));
    }

    throw new MeongnyangerangException(ErrorCode.INVALID_REPORTER_TYPE);
  }
}
