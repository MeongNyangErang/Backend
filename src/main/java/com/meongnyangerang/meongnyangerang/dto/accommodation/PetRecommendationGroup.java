package com.meongnyangerang.meongnyangerang.dto.accommodation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PetRecommendationGroup {

  private Long petId;
  private String petName;
  private List<RecommendationResponse> recommendations;
}
