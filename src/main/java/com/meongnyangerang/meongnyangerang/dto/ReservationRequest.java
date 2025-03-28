package com.meongnyangerang.meongnyangerang.dto;

import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Builder
public class ReservationRequest {

  @NotNull
  private Long roomId;

  @NotNull
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate checkInDate;

  @NotNull
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate checkOutDate;

  @NotNull
  private Integer peopleCount;

  @NotNull
  private Integer petCount;

  @NotBlank
  private String reserverName;

  @NotBlank
  private String reserverPhoneNumber;

  @NotNull
  private Boolean hasVehicle;

  @NotNull
  private Long totalPrice;

  public Reservation toEntity(User user, Room room) {
    return Reservation.builder()
        .user(user)
        .room(room)
        .checkInDate(checkInDate)
        .checkOutDate(checkOutDate)
        .peopleCount(peopleCount)
        .petCount(petCount)
        .reserverName(reserverName)
        .reserverPhoneNumber(reserverPhoneNumber)
        .hasVehicle(hasVehicle)
        .totalPrice(totalPrice)
        .status(ReservationStatus.RESERVED)
        .build();
  }
}
