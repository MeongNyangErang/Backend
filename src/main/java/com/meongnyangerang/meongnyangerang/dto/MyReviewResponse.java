package com.meongnyangerang.meongnyangerang.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyReviewResponse {

  private String accommodationName;
  private Double totalRating;
  private String content;
  private List<ReviewImageResponse> reviewImages;
  private String createdAt;
}
