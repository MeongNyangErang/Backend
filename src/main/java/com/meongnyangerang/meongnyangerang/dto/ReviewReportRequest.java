package com.meongnyangerang.meongnyangerang.dto;

import com.meongnyangerang.meongnyangerang.domain.review.ReportStatus;
import com.meongnyangerang.meongnyangerang.domain.review.ReporterType;
import com.meongnyangerang.meongnyangerang.domain.review.Review;
import com.meongnyangerang.meongnyangerang.domain.review.ReviewReport;
import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReportRequest {

  @NotBlank
  private String reason;

  public ReviewReport toEntity(UserDetailsImpl userDetails, Review review,
      String evidenceImageUrl) {
    return ReviewReport.builder()
        .review(review)
        .reporterId(userDetails.getId())
        .type(userDetails.getRole() == Role.ROLE_USER ? ReporterType.USER : ReporterType.HOST)
        .reason(this.reason)
        .evidenceImageUrl(evidenceImageUrl == null ? "" : evidenceImageUrl)
        .status(ReportStatus.PENDING)
        .build();
  }
}
