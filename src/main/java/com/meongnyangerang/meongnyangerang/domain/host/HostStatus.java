package com.meongnyangerang.meongnyangerang.domain.host;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HostStatus {

  PENDING("승인 대기 중"),
  ACTIVE("활성 상태"),
  DELETED("삭제됨");

  private final String value;
}
