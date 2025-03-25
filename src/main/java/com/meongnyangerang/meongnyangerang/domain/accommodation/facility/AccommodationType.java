package com.meongnyangerang.meongnyangerang.domain.accommodation.facility;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccommodationType {

  HOTEL_RESORT("호텔리조트"),
  DETACHED_HOUSE("독채"),
  FULL_VILLA("풀빌라"),
  PENSION("펜션");

  private final String value;
}
