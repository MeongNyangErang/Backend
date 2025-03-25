package com.meongnyangerang.meongnyangerang.domain.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SenderType {

  USER("일반 회원"),
  HOST("호스트 회원");

  private final String value;
}
