package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.LoginRequest;
import com.meongnyangerang.meongnyangerang.dto.LoginResponse;
import com.meongnyangerang.meongnyangerang.dto.UserSignupRequest;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;

  // 사용자 회원가입 API
  @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Void> registerUser(
      @RequestPart("userInfo") @Valid UserSignupRequest request,
      @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {

    userService.registerUser(request, profileImage);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  // 사용자 로그인 API
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(new LoginResponse(userService.login(request)));
  }

  // 사용자 회원 탈퇴 API
  @DeleteMapping("/me")
  public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
    userService.deleteUser(userDetails.getId());
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
