package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.ReservationRequest;
import com.meongnyangerang.meongnyangerang.dto.UserSignupRequest;
import com.meongnyangerang.meongnyangerang.service.ReservationService;
import com.meongnyangerang.meongnyangerang.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

  @PostMapping("/reservations")
  public ResponseEntity<Void> createReservation(
//      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody ReservationRequest reservationRequest) {

    // 로그인된 사용자의 ID를 받아서 예약을 생성 (Security 적용 후 userDetails.getId())
    reservationService.createReservation(1L, reservationRequest);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

}
