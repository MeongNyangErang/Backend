package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.dto.CustomReservationResponse;
import com.meongnyangerang.meongnyangerang.dto.ReservationRequest;
import com.meongnyangerang.meongnyangerang.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
//      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody ReservationRequest reservationRequest) {

    // 로그인된 사용자의 ID를 받아서 예약을 생성 (Security 적용 후 userDetails.getId())
    reservationService.createReservation(1L, reservationRequest);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @GetMapping("/users/reservations")
  public ResponseEntity<CustomReservationResponse> getReservations(
//      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(defaultValue = "0") Long cursorId,
      @RequestParam(defaultValue = "20") @Range(min = 1, max = 100) int size,
      @RequestParam ReservationStatus status) {

    // 로그인된 사용자의 ID를 받아서 예약을 생성 (Security 적용 후 userDetails.getId())
    CustomReservationResponse response = reservationService.findByStatus(1L, cursorId, size,
        status);

    return ResponseEntity.ok(response);
  }

  @PatchMapping("/users/reservations/{reservationId}/cancel")
  public ResponseEntity<Void> cancelReservation(
//      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @PathVariable Long reservationId) {

    // 로그인된 사용자의 ID를 받아서 예약을 취소 (Security 적용 후 userDetails.getId())
    reservationService.cancelReservation(1L, reservationId);

    return ResponseEntity.ok().build();
  }
}
