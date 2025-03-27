package com.meongnyangerang.meongnyangerang.domain.host;

import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HostStatus {

  PENDING("승인 대기 중"),
  ACTIVE("활성 상태"),
  DELETED("삭제됨");

  private final String value;

  public static void isStatusByCreateAccommodation(HostStatus status){
    if (status != HostStatus.ACTIVE) {
      throw new MeongnyangerangException(ErrorCode.INVALID_AUTHORIZED);
    }
  }
}
