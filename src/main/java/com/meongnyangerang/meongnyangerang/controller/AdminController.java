package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.CustomApplicationResponse;
import com.meongnyangerang.meongnyangerang.dto.LoginRequest;
import com.meongnyangerang.meongnyangerang.dto.LoginResponse;
import com.meongnyangerang.meongnyangerang.dto.PendingHostDetailResponse;
import com.meongnyangerang.meongnyangerang.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

  private final AdminService adminService;

  // 관리자 로그인 API
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(new LoginResponse(adminService.login(request)));
  }

  // 호스트 가입 신청 목록 조회
  @GetMapping("/hosts/pending")
  public ResponseEntity<CustomApplicationResponse> getPendingHostList(
      @RequestParam(defaultValue = "0") Long cursor,
      @RequestParam(defaultValue = "20") @Range(min = 1, max = 100) int size) {

    return ResponseEntity.ok(adminService.getPendingHostList(cursor, size));
  }

  // 호스트 가입 신청 상세 조회
  @GetMapping("/hosts/pending/{hostId}")
  public ResponseEntity<PendingHostDetailResponse> getPendingHostDetail(@PathVariable Long hostId) {

    return ResponseEntity.ok(adminService.getPendingHostDetail(hostId));
  }

  // 호스트 가입 승인
  @PatchMapping("/hosts/{hostId}/approve")
  public ResponseEntity<Void> approveHost(@PathVariable Long hostId) {

    adminService.approveHost(hostId);

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  // 호스트 가입 거절
  @DeleteMapping("/hosts/{hostId}/reject")
  public ResponseEntity<Void> rejectHost(@PathVariable Long hostId) {

    adminService.rejectHost(hostId);

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
