package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.HostService;
import com.meongnyangerang.meongnyangerang.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/account")
public class CommonProfileController {

  private final UserService userService;
  private final HostService hostService;

  // 비밀번호 변경 API(사용자, 호스트 공통 기능)
  @PatchMapping("/password")
  public ResponseEntity<Void> updatePassword(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @Valid @RequestBody PasswordUpdateRequest request) {

    if (userDetails.getRole().equals(Role.ROLE_USER)) {
      userService.updatePassword(userDetails.getId(), request);
    } else if (userDetails.getRole().equals(Role.ROLE_HOST)) {
      hostService.updatePassword(userDetails.getId(), request);
    }

    return ResponseEntity.ok().build();
  }
}
