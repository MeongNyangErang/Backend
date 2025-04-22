package com.meongnyangerang.meongnyangerang.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LatestReviewResponse {

  private Long accommodationId;

  private String accommodationName;

  private String nickname;

  private String content;

  private String imageUrl;

  private Double totalRating;
}
