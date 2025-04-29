package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.EmailRequest;
import com.meongnyangerang.meongnyangerang.dto.VerifyCodeRequest;
import com.meongnyangerang.meongnyangerang.dto.auth.RefreshRequest;
import com.meongnyangerang.meongnyangerang.dto.auth.RefreshResponse;
import com.meongnyangerang.meongnyangerang.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/")
@Validated
public class AuthController {

  private final AuthService authService;

  // 이메일 인증 코드 전송 API
  @PostMapping("/email/send-code")
  public ResponseEntity<Void> sendVerificationCode(@Valid @RequestBody EmailRequest request) {
    authService.sendVerificationCode(request.getEmail());
    return ResponseEntity.ok().build();
  }

  // 인증 코드 검증 API
  @PostMapping("/email/verify-code")
  public ResponseEntity<Void> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
    authService.verifyCode(request.getEmail(), request.getCode());
    return ResponseEntity.ok().build();
  }

  // 사용자 이메일 중복 확인 API
  @GetMapping("/email/check/user")
  public ResponseEntity<Void> checkUserEmail(@RequestParam("email") @Email String email) {
    authService.checkUserEmail(email);
    return ResponseEntity.ok().build();
  }

  // 호스트 이메일 중복 확인 API
  @GetMapping("/email/check/host")
  public ResponseEntity<Void> checkHostEmail(@RequestParam("email") @Email String email) {
    authService.checkHostEmail(email);
    return ResponseEntity.ok().build();
  }

  // 닉네임 중복 확인 API
  @GetMapping("/nickname/check")
  public ResponseEntity<Void> checkNickname(
      @RequestParam("nickname") @Size(min = 2, max = 20) String nickname) {
    authService.checkNickname(nickname);
    return ResponseEntity.ok().build();
  }

  // 리프레시 토큰을 이용해 새로운 액세스 토큰을 재발급하는 API
  @PostMapping("/auth/reissue")
  public ResponseEntity<RefreshResponse> reissueAccessToken(
      @RequestBody @Valid RefreshRequest request) {
    return ResponseEntity.ok(authService.reissueAccessToken(request.refreshToken()));
  }
}
