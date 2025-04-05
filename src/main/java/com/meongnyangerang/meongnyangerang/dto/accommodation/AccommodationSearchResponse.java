package com.meongnyangerang.meongnyangerang.dto.accommodation;

import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccommodationSearchResponse {

  private Long accommodationId;
  private String accommodationName;
  private String address;
  private String detailedAddress;
  private String thumbnailUrl;
  private Double totalRating;
  private Long price;
  private AccommodationType accommodationType;

}
