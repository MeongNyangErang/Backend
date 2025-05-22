package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.dto.HostReservationResponse;
import com.meongnyangerang.meongnyangerang.dto.ReservationRequest;
import com.meongnyangerang.meongnyangerang.dto.ReservationResponse;
import com.meongnyangerang.meongnyangerang.dto.UserReservationResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
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

  @PostMapping("/users/reservations/validate")
  public ResponseEntity<Void> validateReservation(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @Valid @RequestBody ReservationRequest request) {

    reservationService.validateReservation(userDetails.getId(), request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/users/reservations")
  public ResponseEntity<ReservationResponse> createReservation(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @Valid @RequestBody ReservationRequest request) {

    ReservationResponse response = reservationService.createReservation(userDetails.getId(),
        request);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/users/reservations")
  public ResponseEntity<PageResponse<UserReservationResponse>> getUserReservation(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @PageableDefault(size = 20, sort = "createdAt", direction = Direction.DESC) Pageable pageable,
      @RequestParam ReservationStatus status) {

    return ResponseEntity.ok(reservationService.getUserReservations(
        userDetails.getId(), pageable, status));
  }

  @PatchMapping("/users/reservations/{reservationId}/cancel")
  public ResponseEntity<Void> cancelReservation(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @PathVariable Long reservationId) {

    reservationService.cancelReservation(userDetails.getId(), reservationId);

    return ResponseEntity.ok().build();
  }

  @GetMapping("/hosts/reservations")
  public ResponseEntity<PageResponse<HostReservationResponse>> getHostReservation(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @PageableDefault(size = 20, sort = "createdAt", direction = Direction.DESC) Pageable pageable,
      @RequestParam ReservationStatus status) {

    return ResponseEntity.ok(reservationService.getHostReservation(
        userDetails.getId(), pageable, status));
  }
}
