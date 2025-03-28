package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.HostSignupRequest;
import com.meongnyangerang.meongnyangerang.service.HostService;
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
@RequestMapping("/api/v1/hosts")
public class HostController {

  private final HostService hostService;

  // 호스트 회원가입 API
  @PostMapping("/signup")
  public ResponseEntity<Void> registerHost(@Valid @RequestBody HostSignupRequest request) {
    hostService.registerHost(request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}
