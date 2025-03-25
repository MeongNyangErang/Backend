package com.meongnyangerang.meongnyangerang.domain.room.facility;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoomFacilityType {

  STYLER("스타일러"),
  REFRIGERATOR("냉장고"),
  RICE_COOKER("전기밥솥"),
  SHOWER_ROOM("샤워실"),
  AIR_CONDITIONER("에어컨"),
  TV("TV"),
  WIFI("와이파이"),
  BATHROOM_SUPPLIES("욕실용품"),
  DRYER("드라이기"),
  BARBECUE("바베큐"),
  POSSIBLE_COOK_IN_ROOM("객실 내 취사");

  private final String value;
}
