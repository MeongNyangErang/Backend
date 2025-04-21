package com.meongnyangerang.meongnyangerang.dto;

import com.meongnyangerang.meongnyangerang.domain.review.ReportStatus;
import com.meongnyangerang.meongnyangerang.domain.review.ReporterType;
import com.meongnyangerang.meongnyangerang.domain.review.ReviewReport;
import java.time.LocalDateTime;

public record ReviewReportResponse(
    Long reviewId,
    Long reporterId,
    ReporterType reporterType,
    String reason,
    String evidenceImageUrl,
    ReportStatus status,
    LocalDateTime createdAt
) {

  public static ReviewReportResponse from(ReviewReport reviewReport) {
    return new ReviewReportResponse(
        reviewReport.getId(),
        reviewReport.getReporterId(),
        reviewReport.getType(),
        reviewReport.getReason(),
        reviewReport.getEvidenceImageUrl(),
        reviewReport.getStatus(),
        reviewReport.getCreatedAt()
    );
  }
}
