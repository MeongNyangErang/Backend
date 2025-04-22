package com.meongnyangerang.meongnyangerang.dto.accommodation;

import com.meongnyangerang.meongnyangerang.domain.AccommodationRoomDocument;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AccommodationSearchResponse {

  private Long accommodationId;
  private String accommodationName;
  private String address;
  private boolean isWishlisted;
  private String thumbnailUrl;
  private Double totalRating;
  private Long price;
  private Integer standardPeopleCount;
  private Integer standardPetCount;
  private AccommodationType accommodationType;

  public static AccommodationSearchResponse fromDocument(AccommodationRoomDocument doc,
      boolean isWishlisted) {

    return AccommodationSearchResponse.builder()
        .accommodationId(doc.getAccommodationId())
        .accommodationName(doc.getAccommodationName())
        .address(doc.getAddress())
        .isWishlisted(isWishlisted)
        .thumbnailUrl(doc.getThumbnailUrl())
        .totalRating(doc.getTotalRating())
        .price(doc.getPrice())
        .standardPeopleCount(doc.getStandardPeopleCount())
        .standardPetCount(doc.getStandardPetCount())
        .accommodationType(doc.getAccommodationType())
        .build();
  }
}
