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
  private String detailedAddress;
  private String thumbnailUrl;
  private Double totalRating;
  private Long price;
  private AccommodationType accommodationType;

  public static AccommodationSearchResponse fromDocument(AccommodationRoomDocument doc) {
    return AccommodationSearchResponse.builder()
        .accommodationId(doc.getAccommodationId())
        .accommodationName(doc.getAccommodationName())
        .address(doc.getAddress())
        .detailedAddress(doc.getDetailedAddress)
        .thumbnailUrl(doc.getThumbanilUrl)
        .totalRating(doc.getTotalRating())
        .price(doc.getPrice())
        .accommodationType(doc.getAccommodationType())
        .build();
  }
}
