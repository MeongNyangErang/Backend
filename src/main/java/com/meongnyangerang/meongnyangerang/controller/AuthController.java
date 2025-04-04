package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.EmailRequest;
import com.meongnyangerang.meongnyangerang.dto.NicknameRequest;
import com.meongnyangerang.meongnyangerang.dto.VerifyCodeRequest;
import com.meongnyangerang.meongnyangerang.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/")
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

  // 이메일 중복 확인 API
  @GetMapping("/email/check")
  public ResponseEntity<Void> checkEmail(@Valid @RequestBody EmailRequest request) {
    authService.checkEmail(request.getEmail());
    return ResponseEntity.ok().build();
  }

  // 닉네임 중복 확인 API
  @GetMapping("/nickname/check")
  public ResponseEntity<Void> checkNickname(@Valid @RequestBody NicknameRequest request) {
    authService.checkNickname(request.getNickname());
    return ResponseEntity.ok().build();
  }
}
