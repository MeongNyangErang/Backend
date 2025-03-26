package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.EmailRequest;
import com.meongnyangerang.meongnyangerang.service.UserService;
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

  // 이메일 인증 코드 전송
  @PostMapping("/email/send-code")
  public ResponseEntity<String> sendVerificationCode(@RequestBody EmailRequest request) {
    userService.sendVerificationCode(request.getEmail());
    return ResponseEntity.ok("인증코드가 발송되었습니다.");
  }

}
