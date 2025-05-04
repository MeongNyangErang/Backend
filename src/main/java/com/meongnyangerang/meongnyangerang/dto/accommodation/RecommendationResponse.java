package com.meongnyangerang.meongnyangerang.dto.accommodation;

import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationDocument;
import java.util.Set;
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

  private boolean isWishlisted;

  // AccommodationDocument를 응답 객체로 변환
  public static RecommendationResponse from(AccommodationDocument doc) {
    return RecommendationResponse.builder()
        .id(doc.getId())
        .name(doc.getName())
        .price(doc.getPrice())
        .totalRating(doc.getTotalRating())
        .thumbnailUrl(doc.getThumbnailUrl())
        .build();
  }

  // 기존 메서드 오버로딩: 찜 여부 포함
  public static RecommendationResponse from(AccommodationDocument doc, Set<Long> wishlistedIds) {
    return RecommendationResponse.builder()
        .id(doc.getId())
        .name(doc.getName())
        .price(doc.getPrice())
        .totalRating(doc.getTotalRating())
        .thumbnailUrl(doc.getThumbnailUrl())
        .isWishlisted(wishlistedIds.contains(doc.getId()))
        .build();
  }
}
