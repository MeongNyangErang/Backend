package com.meongnyangerang.meongnyangerang.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 반려동물 유형
@Getter
@RequiredArgsConstructor
public enum PetType {
  SMALL_DOG("소형견"),
  MEDIUM_DOG("중형견"),
  RGE_DOG("대형견"),
  CAT("고양이");

  private final String value;
}
