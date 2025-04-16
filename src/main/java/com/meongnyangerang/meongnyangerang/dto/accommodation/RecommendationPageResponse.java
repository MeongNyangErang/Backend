package com.meongnyangerang.meongnyangerang.dto.accommodation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecommendationPageResponse {

  private List<RecommendationResponse> content;

  private int pageNumber;

  private int totalPages;

  private boolean isLast;

}