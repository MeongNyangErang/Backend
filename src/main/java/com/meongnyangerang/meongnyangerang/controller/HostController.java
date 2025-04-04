package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.HostSignupRequest;
import com.meongnyangerang.meongnyangerang.dto.LoginRequest;
import com.meongnyangerang.meongnyangerang.dto.LoginResponse;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.HostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/hosts")
public class HostController {

  private final HostService hostService;

  // 호스트 회원가입 API
  @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Void> registerHost(
      @RequestPart("hostInfo") @Valid HostSignupRequest request,
      @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
      @RequestPart("businessLicense") MultipartFile businessLicenseImage,
      @RequestPart("submitDocument") MultipartFile submitDocumentImage
  ) {
    hostService.registerHost(request, profileImage, businessLicenseImage, submitDocumentImage);
    return ResponseEntity.ok().build();
  }

  // 호스트 로그인 API
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(new LoginResponse(hostService.login(request)));
  }

  // 호스트 회원 탈퇴 API
  @DeleteMapping("/me")
  public ResponseEntity<Void> deleteHost(@AuthenticationPrincipal UserDetailsImpl userDetails) {
    hostService.deleteHost(userDetails.getId());
    return ResponseEntity.ok().build();
  }
}
