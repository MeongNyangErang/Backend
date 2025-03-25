package com.meongnyangerang.meongnyangerang.domain.room;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HashtagType {

  FAMILY_TRIP("가족여행"),
  SPA("스파"),
  OCEAN_VIEW("오션뷰"),
  PARTY_ROOM("파티룸"),
  COZY("아늑한"),
  MODERN("모던한"),
  NO_SMOKING("금연숙소"),
  FOREST_VIEW("포레스트뷰"),
  EMOTIONAL("감성숙소");

  private final String value;
}
