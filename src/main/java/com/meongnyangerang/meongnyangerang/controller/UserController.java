package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.dto.CustomReservationResponse;
import com.meongnyangerang.meongnyangerang.dto.LoginRequest;
import com.meongnyangerang.meongnyangerang.dto.LoginResponse;
import com.meongnyangerang.meongnyangerang.dto.ReservationRequest;
import com.meongnyangerang.meongnyangerang.dto.UserSignupRequest;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.ReservationService;
import com.meongnyangerang.meongnyangerang.service.UserService;
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
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;
  private final ReservationService reservationService;

  // 사용자 회원가입 API
  @PostMapping("/signup")
  public ResponseEntity<Void> registerUser(@Valid @RequestBody UserSignupRequest request) {
    userService.registerUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  // 사용자 로그인 API
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    String token = userService.login(request);
    return ResponseEntity.ok(new LoginResponse(token));
  }

  @PostMapping("/reservations")
  public ResponseEntity<Void> createReservation(
//      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody ReservationRequest reservationRequest) {

    // 로그인된 사용자의 ID를 받아서 예약을 생성 (Security 적용 후 userDetails.getId())
    reservationService.createReservation(1L, reservationRequest);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @GetMapping("/reservations")
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

  @PatchMapping("/reservations/{reservationId}/cancel")
  public ResponseEntity<Void> cancelReservation(
//      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @PathVariable Long reservationId) {

    // 로그인된 사용자의 ID를 받아서 예약을 취소 (Security 적용 후 userDetails.getId())
    reservationService.cancelReservation(1L, reservationId);

    return ResponseEntity.ok().build();
  }
}
