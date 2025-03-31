package com.meongnyangerang.meongnyangerang.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HostReservationResponse {

  private String reservationDate;
  private String reserverName;
  private String reserverPhoneNumber;
  private boolean hasVehicle;
  private String checkInDate;
  private String checkOutDate;
  private int peopleCount;
  private int petCount;
  private long totalPrice;
}
