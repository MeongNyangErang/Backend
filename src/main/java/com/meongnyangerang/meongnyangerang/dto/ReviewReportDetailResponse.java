package com.meongnyangerang.meongnyangerang.dto;

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

}
