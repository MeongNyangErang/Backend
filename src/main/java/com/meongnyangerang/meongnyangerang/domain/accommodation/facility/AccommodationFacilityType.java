package com.meongnyangerang.meongnyangerang.domain.accommodation.facility;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccommodationFacilityType {

  CONVENIENCE_STORE("편의점"),
  PUBLIC_SWIMMING_POOL("공용 수영장"),
  BARBECUE("바베큐"),
  FITNESS("피트니스"),
  KARAOKE_ROOM("노래방"),
  WIFI("와이파이"),
  PARKING_LOT("주차장"),
  FREE_PARKING("무료주차"),
  PAID_PARKING("유료주차"),
  BREAKFAST("조식"),
  PICKUP("픽업 서비스"),
  FOOT_VOLLEYBALL_COURT("족구장");

  private final String value;
}
