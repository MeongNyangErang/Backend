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
  private final ReviewDeletionService reviewDeletionService;
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
  public void deleteReviewReport(Long reviewReportId) {
    ReviewReport reviewReport = reviewReportRepository.findById(reviewReportId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.NOT_EXIST_REVIEW_REPORT));

    reviewReportRepository.delete(reviewReport);

    reviewDeletionService.deleteReviewCompletely(reviewReport.getReview());
  }

  private String getReporterNickname(Long reporterId, ReporterType type) {
    String nickname = "";

    if (type == ReporterType.USER) {
      nickname = String.valueOf(userRepository.findById(reporterId).map(User::getNickname));
    } else if (type == ReporterType.HOST) {
      nickname = String.valueOf(hostRepository.findById(reporterId).map(Host::getNickname));
    }

    return nickname;
  }
}
