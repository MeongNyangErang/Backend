package com.meongnyangerang.meongnyangerang.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 반려동물 활동성
@Getter
@RequiredArgsConstructor
public enum ActivityLevel {

  LOW("낮음"),
  MEDIUM("보통"),
  HIGH("높음");

  private final String value;
}
