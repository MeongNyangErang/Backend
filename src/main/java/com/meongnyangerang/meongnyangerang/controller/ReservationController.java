package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.dto.CustomReservationResponse;
import com.meongnyangerang.meongnyangerang.dto.HostReservationResponse;
import com.meongnyangerang.meongnyangerang.dto.ReservationRequest;
import com.meongnyangerang.meongnyangerang.dto.UserReservationResponse;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReservationController {

  private final ReservationService reservationService;

  @PostMapping("/users/reservations")
  public ResponseEntity<Void> createReservation(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @Valid @RequestBody ReservationRequest reservationRequest) {

    reservationService.createReservation(userDetails.getId(), reservationRequest);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @GetMapping("/users/reservations")
  public ResponseEntity<CustomReservationResponse<UserReservationResponse>> getUserReservation(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @RequestParam(defaultValue = "0") Long cursor,
      @RequestParam(defaultValue = "20") @Range(min = 1, max = 100) int size,
      @RequestParam ReservationStatus status) {

    return ResponseEntity.ok(reservationService.getUserReservations(
        userDetails.getId(), cursor, size,
        status));
  }

  @PatchMapping("/users/reservations/{reservationId}/cancel")
  public ResponseEntity<Void> cancelReservation(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @PathVariable Long reservationId) {

    reservationService.cancelReservation(userDetails.getId(), reservationId);

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @GetMapping("/hosts/reservations")
  public ResponseEntity<CustomReservationResponse<HostReservationResponse>> getHostReservation(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @RequestParam(defaultValue = "0") Long cursor,
      @RequestParam(defaultValue = "20") @Range(min = 1, max = 100) int size,
      @RequestParam ReservationStatus status) {

    return ResponseEntity.ok(reservationService.getHostReservation(
        userDetails.getId(), cursor, size,
        status));
  }
}
