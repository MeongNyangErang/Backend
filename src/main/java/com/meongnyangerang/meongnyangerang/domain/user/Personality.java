package com.meongnyangerang.meongnyangerang.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 반려동물 성향
@Getter
@RequiredArgsConstructor
public enum Personality {

  INTROVERT("내향적"),
  EXTROVERT("외향적");

  private final String value;
}
