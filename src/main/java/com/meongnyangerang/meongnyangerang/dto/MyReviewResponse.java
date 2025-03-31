package com.meongnyangerang.meongnyangerang.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyReviewResponse {

  private String accommodationName;
  private Double totalRating;
  private String content;
  private String reviewImageUrl;
  private String createdAt;

}
