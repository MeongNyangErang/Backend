package com.meongnyangerang.meongnyangerang.domain.reservation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationStatus {
  RESERVED("예약됨"),
  COMPLETED("이용 완료"),
  CANCELED("취소됨");

  private final String value;
}
