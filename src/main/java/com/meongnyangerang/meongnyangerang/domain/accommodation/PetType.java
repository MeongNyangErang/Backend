package com.meongnyangerang.meongnyangerang.domain.accommodation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PetType {

  LARGE_DOG("대형견"),
  MEDIUM_DOG("중형견"),
  SMALL_DOG("소형견"),
  CAT("고양이");

  private final String value;
}
