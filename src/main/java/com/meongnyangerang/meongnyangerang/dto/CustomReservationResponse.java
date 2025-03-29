package com.meongnyangerang.meongnyangerang.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomReservationResponse {

  private List<ReservationResponse> content;
  private String cursor;
  private boolean hasNext;

}
