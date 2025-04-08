package com.meongnyangerang.meongnyangerang.dto.accommodation;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DefaultRecommendationResponse {

  private Long id;

  private String accommodationName;

  private Long price;

  private Double totalRating;

}
