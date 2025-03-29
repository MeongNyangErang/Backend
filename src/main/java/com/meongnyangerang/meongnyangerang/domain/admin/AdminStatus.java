package com.meongnyangerang.meongnyangerang.domain.admin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminStatus {

  ACTIVE("활성 상태");

  private final String value;
}
