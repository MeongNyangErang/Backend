package com.meongnyangerang.meongnyangerang.domain.user;

import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;

public enum Role {
  ROLE_USER,
  ROLE_HOST,
  ROLE_ADMIN;

  public SenderType toSenderType() {
    return switch (this) {
      case ROLE_USER -> SenderType.USER;
      case ROLE_HOST -> SenderType.HOST;
      default -> throw new MeongnyangerangException(ErrorCode.INVALID_AUTHORIZED);
    };
  }
}
