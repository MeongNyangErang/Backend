package com.meongnyangerang.meongnyangerang.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReservationResponse {

  private String reservationDate;
  private String accommodationName;
  private String roomName;
  private String checkInDate;
  private String checkOutDate;
  private String checkInTime;
  private String checkOutTime;
  private int peopleCount;
  private int petCount;
  private long totalPrice;
}
