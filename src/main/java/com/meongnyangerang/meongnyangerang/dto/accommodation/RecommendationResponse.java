package com.meongnyangerang.meongnyangerang.dto.accommodation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {

  private Long id;

  private String name;

  private Long price;

  private Double totalRating;

  private String thumbnailUrl;

}
