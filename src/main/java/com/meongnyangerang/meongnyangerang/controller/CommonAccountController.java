package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.dto.NicknameUpdateRequest;
import com.meongnyangerang.meongnyangerang.dto.PasswordUpdateRequest;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/account")
public class CommonAccountController {

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

  // 닉네임 변경 API(사용자, 호스트 공통 기능)
  @PatchMapping("/nickname")
  public ResponseEntity<Void> updateNickname(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @Valid @RequestBody NicknameUpdateRequest request
  ) {
    if (userDetails.getRole().equals(Role.ROLE_USER)) {
      userService.updateNickname(userDetails.getId(), request.newNickname());
    } else if (userDetails.getRole().equals(Role.ROLE_HOST)) {
      hostService.updateNickname(userDetails.getId(), request.newNickname());
    }

    return ResponseEntity.ok().build();
  }

  // 프로필 사진 변경 API(사용자, 호스트 공통 기능)
  @PatchMapping
  public ResponseEntity<Void> updateProfileImage(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @RequestPart(value = "newProfileImage")MultipartFile newProfileImage
  ) {
    if (userDetails.getRole().equals(Role.ROLE_USER)) {
      userService.updateProfileImage(userDetails.getId(), newProfileImage);
    } else if (userDetails.getRole().equals(Role.ROLE_HOST)) {
      hostService.updateProfileImage(userDetails.getId(), newProfileImage);
    }
    return ResponseEntity.ok().build();
  }
}
