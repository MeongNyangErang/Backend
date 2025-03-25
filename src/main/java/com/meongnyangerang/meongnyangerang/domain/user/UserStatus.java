package com.meongnyangerang.meongnyangerang.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {

  ACTIVE("활성 상태"),
  DELETED("삭제됨");

  private final String value;
}
