package com.meongnyangerang.meongnyangerang.dto;

import com.meongnyangerang.meongnyangerang.domain.review.ReviewReport;
import java.time.format.DateTimeFormatter;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewReportDetailResponse {

  private Long reviewReportId;
  private Long reviewId;
  private String reviewerNickname;
  private String reporterNickname;
  private String reason;
  private Integer reportCount;
  private String evidenceImageUrl;
  private String reportDate;

  public static ReviewReportDetailResponse from(ReviewReport reviewReport,
      String reporterNickname) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    return ReviewReportDetailResponse.builder()
        .reviewReportId(reviewReport.getId())
        .reviewId(reviewReport.getReview().getId())
        .reviewerNickname(reviewReport.getReviewerNickname())
        .reporterNickname(reporterNickname)
        .reason(reviewReport.getReason())
        .reportCount(reviewReport.getReview().getReportCount())
        .evidenceImageUrl(reviewReport.getEvidenceImageUrl())
        .reportDate(reviewReport.getCreatedAt().format(formatter))
        .build();
  }
}
