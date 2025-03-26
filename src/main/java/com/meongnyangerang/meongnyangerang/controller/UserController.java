package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.EmailRequest;
import com.meongnyangerang.meongnyangerang.dto.UserSignupRequest;
import com.meongnyangerang.meongnyangerang.dto.VerifyCodeRequest;
import com.meongnyangerang.meongnyangerang.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

  // 이메일 인증 코드 전송 API
  @PostMapping("/email/send-code")
  public ResponseEntity<String> sendVerificationCode(@Valid @RequestBody EmailRequest request) {
    userService.sendVerificationCode(request.getEmail());
    return ResponseEntity.ok("인증코드가 발송되었습니다.");
  }

  // 인증 코드 검증 API
  @PostMapping("/email/verify-code")
  public ResponseEntity<String> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
    userService.verifyCode(request.getEmail(), request.getCode());
    return ResponseEntity.ok("인증이 완료되었습니다.");
  }

  // 사용자 회원가입 API
  @PostMapping("/signup")
  public ResponseEntity<String> registerUser(@Valid @RequestBody UserSignupRequest request) {
    userService.registerUser(request);
    return ResponseEntity.ok("회원가입이 완료되었습니다.");
  }

}
