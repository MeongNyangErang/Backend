package com.meongnyangerang.meongnyangerang.domain.room.facility;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoomPetFacilityType {

  FOOD_BOWL("식기"),
  EXCLUSIVE_YARD("전용 마당"),
  POTTY_SUPPLIES("배변용품"),
  TOY("장난감"),
  BED("침대"),
  ANTI_SLIP_FLOOR("미끄럼 방지 바닥"),
  FENCE_AREA("펜스 설치 공간"),
  CAT_TOWER("캣타워"),
  CAT_WHEEL("캣휠"),
  BRUSH("브러쉬"),
  PET_STEPS("강아지 계단");

  private final String value;
}
