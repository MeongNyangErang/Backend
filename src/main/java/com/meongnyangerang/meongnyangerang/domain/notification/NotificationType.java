package com.meongnyangerang.meongnyangerang.domain.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

  MESSAGE("메세지"),
  RESERVATION_CONFIRMED("예약 확인"), // 사용자에게
  RESERVATION_REGISTERED("예약 등록"), // 호스트에게
  RESERVATION_REMINDER("예약 리마인더"),
  REVIEW("리뷰");

  private final String value;
}
