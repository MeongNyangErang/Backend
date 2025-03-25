package com.meongnyangerang.meongnyangerang.domain.accommodation.facility;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccommodationPetFacilityType {

  EXERCISE_AREA("대형 운동장"),
  EXCLUSIVE_YARD("전용 마당"),
  PLAYGROUND("놀이터"),
  SHOWER_ROOM("샤워장"),
  SWIMMING_POOL("수영장"),
  ANTI_SLIP_FLOOR("미끄럼 방지 바닥"),
  FENCE_AREA("펜스 설치 공간"),
  CARE_SERVICE("돌봄 서비스"),
  PET_FOOD("펫 푸드 제공"),
  NEARBY_HOSPITAL("인근 동물병원");

  private final String value;
}
