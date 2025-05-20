package com.meongnyangerang.meongnyangerang.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.review.ReportStatus;
import com.meongnyangerang.meongnyangerang.domain.review.ReporterType;
import com.meongnyangerang.meongnyangerang.domain.review.Review;
import com.meongnyangerang.meongnyangerang.domain.review.ReviewReport;
import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.UserStatus;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewReportRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.ReviewDeletionService;
import com.meongnyangerang.meongnyangerang.service.ReviewReportService;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ReviewReportServiceTest {

  @Mock
  private ReviewReportRepository reviewReportRepository;

  @Mock
  private ReviewRepository reviewRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private HostRepository hostRepository;

  @Mock
  private ReviewDeletionService reviewDeletionService;

  @Mock
  private ImageService imageService;

  @InjectMocks
  private ReviewReportService reviewReportService;

  @Test
  @DisplayName("리뷰 신고 - 성공 (사진 O)")
  void createReport_success() {
    // given
    long reviewId = 1L;

    Review review = Review.builder().id(reviewId).reportCount(0).build();
    ReviewReportRequest request = ReviewReportRequest.builder().reason("욕설을 사용합니다.").build();
    MockMultipartFile file = new MockMultipartFile("image", "test-image.jpg", "image/jpeg",
        "image content".getBytes());

    when(reviewRepository.findById(reviewId)).thenReturn(Optional.ofNullable(review));
    when(reviewReportRepository.existsByReporterIdAndReview(1L, review)).thenReturn(false);
    when(imageService.storeImage(file)).thenReturn(
        "https://storage.example.com/images/test-image.jpg");

    ArgumentCaptor<ReviewReport> captor = ArgumentCaptor.forClass(ReviewReport.class);

    // when
    reviewReportService.createReport(
        new UserDetailsImpl(1L, "test@gmail.com", "test1234!", Role.ROLE_USER,
            UserStatus.valueOf("ACTIVE")), reviewId, request, file);

    // then
    verify(reviewReportRepository, times(1)).save(captor.capture());
    ReviewReport saved = captor.getValue();
    assertEquals(1L, saved.getReporterId());
    assertEquals(review, saved.getReview());
    assertEquals(1, review.getReportCount());
    assertEquals("https://storage.example.com/images/test-image.jpg", saved.getEvidenceImageUrl());
  }

  @Test
  @DisplayName("리뷰 신고 - 성공 (사진 X)")
  void createReport_image_null_success() {
    // given
    long reviewId = 1L;

    Review review = Review.builder().id(reviewId).reportCount(0).build();
    ReviewReportRequest request = ReviewReportRequest.builder().reason("욕설을 사용합니다.").build();
    MockMultipartFile file = null;

    when(reviewRepository.findById(reviewId)).thenReturn(Optional.ofNullable(review));
    when(reviewReportRepository.existsByReporterIdAndReview(1L, review)).thenReturn(false);

    ArgumentCaptor<ReviewReport> captor = ArgumentCaptor.forClass(ReviewReport.class);

    // when
    reviewReportService.createReport(
        new UserDetailsImpl(1L, "test@gmail.com", "test1234!", Role.ROLE_USER,
            UserStatus.valueOf("ACTIVE")), reviewId, request, file);

    // then
    verify(reviewReportRepository, times(1)).save(captor.capture());
    verify(imageService, times(0)).storeImage(file);
    ReviewReport saved = captor.getValue();
    assertEquals(1L, saved.getReporterId());
    assertEquals(review, saved.getReview());
    assertEquals(1, review.getReportCount());
  }

  @Test
  @DisplayName("리뷰 신고 - 실패 (리뷰 존재 X)")
  void createReport_review_not_found() {
    // given
    long reviewId = 1L;

    ReviewReportRequest request = ReviewReportRequest.builder().reason("욕설을 사용합니다.").build();
    MockMultipartFile file = new MockMultipartFile("image", "test-image.jpg", "image/jpeg",
        "image content".getBytes());

    when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reviewReportService.createReport(
          new UserDetailsImpl(1L, "test@gmail.com", "test1234!", Role.ROLE_USER,
              UserStatus.valueOf("ACTIVE")), reviewId, request, file);
    });

    // then
    assertEquals(ErrorCode.REVIEW_NOT_FOUND, e.getErrorCode());
  }

  @Test
  @DisplayName("리뷰 신고 - 실패 (리뷰 신고 존재 O)")
  void createReport_review_report_already_exists() {
    // given
    long reviewId = 1L;

    Review review = Review.builder().id(reviewId).reportCount(0).build();
    ReviewReportRequest request = ReviewReportRequest.builder().reason("욕설을 사용합니다.").build();
    MockMultipartFile file = new MockMultipartFile("image", "test-image.jpg", "image/jpeg",
        "image content".getBytes());

    when(reviewRepository.findById(reviewId)).thenReturn(Optional.ofNullable(review));
    when(reviewReportRepository.existsByReporterIdAndReview(1L, review)).thenReturn(true);

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reviewReportService.createReport(
          new UserDetailsImpl(1L, "test@gmail.com", "test1234!", Role.ROLE_USER,
              UserStatus.valueOf("ACTIVE")), reviewId, request, file);
    });

    // then
    assertEquals(ErrorCode.REVIEW_REPORT_ALREADY_EXISTS, e.getErrorCode());
  }

  @Test
  @DisplayName("신고 리뷰 삭제 - 성공")
  void deleteReviewReport_success() {
    // given
    Review review = Review.builder().id(1L).build();
    ReviewReport reviewReport = ReviewReport.builder().id(1L).review(review).build();

    when(reviewReportRepository.findById(1L)).thenReturn(Optional.ofNullable(reviewReport));

    // when
    reviewReportService.deleteReviewReport(1L);

    // then
    verify(reviewDeletionService, times(1)).deleteReviewCompletely(review);
    verify(reviewReportRepository, times(1)).delete(reviewReport);
  }

  @Test
  @DisplayName("신고 리뷰 삭제 - 실패: 신고 리뷰가 없는 경우")
  void deleteReviewReport_not_exists_review_report() {
    // given
    when(reviewReportRepository.findById(999L)).thenReturn(Optional.empty());

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reviewReportService.deleteReviewReport(999L);
    });

    // then
    assertEquals(ErrorCode.NOT_EXIST_REVIEW_REPORT, e.getErrorCode());
  }

  @Test
  @DisplayName("신고된 리뷰 상세 보기 - 성공")
  void getReviewReportDetail_success() {
    // given
    User user = User.builder().id(1L).nickname("장난꾸러기").build();
    Host host = Host.builder().id(100L).nickname("3월").build();
    Review review = Review.builder().id(1L).user(user).content("비추천. 최악의 숙소입니다.").build();

    ReviewReport reviewReport = ReviewReport.builder()
        .id(1L)
        .review(review)
        .reporterId(host.getId())
        .type(ReporterType.HOST)
        .status(ReportStatus.PENDING)
        .reason("나쁜 말만 사용합니다.")
        .evidenceImageUrl("")
        .createdAt(LocalDateTime.of(2025, 5, 5, 15, 30, 0))
        .build();

    when(reviewReportRepository.findById(reviewReport.getId())).thenReturn(
        Optional.of(reviewReport));
    when(hostRepository.findById(host.getId())).thenReturn(Optional.of(host));

    // when
    ReviewReportDetailResponse response = reviewReportService.getReviewReportDetail(
        reviewReport.getId());

    // then
    assertEquals(review.getId(), response.getReviewId());
    assertEquals(user.getNickname(), response.getReviewerNickname());
    assertEquals(host.getNickname(), response.getReporterNickname());
    assertEquals("", response.getEvidenceImageUrl());
    assertEquals("나쁜 말만 사용합니다.", response.getReason());
    assertEquals("2025-05-05", response.getReportDate());
  }

  @Test
  @DisplayName("신고된 리뷰 상세 보기 - 실패: 신고 리뷰가 없는 경우")
  void getReviewReportDetail_not_exists_review_report() {
    // given
    when(reviewReportRepository.findById(2L)).thenReturn(Optional.empty());

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reviewReportService.getReviewReportDetail(2L);
    });

    // then
    assertEquals(ErrorCode.NOT_EXIST_REVIEW_REPORT, e.getErrorCode());
  }

  @Test
  @DisplayName("신고된 리뷰 상세 보기 - 실패: 이미 처리된 신고인 경우")
  void getReviewReportDetail_already_processed_review_report() {
    // given
    User user = User.builder().id(1L).nickname("장난꾸러기").build();
    Host host = Host.builder().id(100L).nickname("3월").build();
    Review review = Review.builder().id(1L).user(user).content("비추천. 최악의 숙소입니다.").build();

    ReviewReport reviewReport = ReviewReport.builder()
        .id(1L)
        .review(review)
        .reporterId(host.getId())
        .type(ReporterType.HOST)
        .status(ReportStatus.COMPLETED)
        .reason("나쁜 말만 사용합니다.")
        .evidenceImageUrl("")
        .createdAt(LocalDateTime.of(2025, 5, 5, 15, 30, 0))
        .build();

    when(reviewReportRepository.findById(reviewReport.getId())).thenReturn(
        Optional.of(reviewReport));

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reviewReportService.getReviewReportDetail(reviewReport.getId());
    });

    // then
    assertEquals(ErrorCode.ALREADY_PROCESSED_REVIEW_REPORT, e.getErrorCode());
  }
}