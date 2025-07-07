package com.meongnyangerang.meongnyangerang.scheduler;

import com.meongnyangerang.meongnyangerang.domain.review.Review;
import com.meongnyangerang.meongnyangerang.repository.ReviewReportRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewRepository;
import com.meongnyangerang.meongnyangerang.service.ReviewDeletionService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewReportScheduler {

  private final ReviewRepository reviewRepository;
  private final ReviewReportRepository reviewReportRepository;
  private final ReviewDeletionService reviewDeletionService;

  // 매일 새벽 3시에 실행
  @Scheduled(cron = "0 0 3 * * ?")
  public void deleteOldHiddenReviews() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(7);

    // 숨김 처리된 리뷰 중, 숨김 시간이 7일 이상 경과한 리뷰 조회
    List<Review> oldHiddenReviews = reviewRepository.findByHiddenTrueAndHiddenAtBefore(cutoff);

    // 관련된 리뷰 신고 삭제 후, 리뷰 이미지 및 리뷰 삭제
    for (Review review : oldHiddenReviews) {
      reviewReportRepository.deleteAllByReview_Id(review.getId());
      reviewDeletionService.deleteReviewCompletely(review);
    }
  }
}
