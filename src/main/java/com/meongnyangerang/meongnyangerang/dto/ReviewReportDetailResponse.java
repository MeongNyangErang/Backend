package com.meongnyangerang.meongnyangerang.dto;

import com.meongnyangerang.meongnyangerang.domain.review.ReviewReport;
import java.time.format.DateTimeFormatter;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewReportDetailResponse {

  private Long reviewId;
  private String reviewerNickname;
  private String reporterNickname;
  private String reason;
  private String evidenceImageUrl;
  private String reportDate;

  public static ReviewReportDetailResponse from(ReviewReport reviewReport,
      String reporterNickname) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    return ReviewReportDetailResponse.builder()
        .reviewId(reviewReport.getReview().getId())
        .reviewerNickname(reviewReport.getReviewerNickname())
        .reporterNickname(reporterNickname)
        .reason(reviewReport.getReason())
        .evidenceImageUrl(reviewReport.getEvidenceImageUrl())
        .reportDate(reviewReport.getCreatedAt().format(formatter))
        .build();
  }
}
