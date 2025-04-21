package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.domain.review.ReviewReport;
import com.meongnyangerang.meongnyangerang.dto.LoginRequest;
import com.meongnyangerang.meongnyangerang.dto.LoginResponse;
import com.meongnyangerang.meongnyangerang.dto.PendingHostDetailResponse;
import com.meongnyangerang.meongnyangerang.dto.PendingHostListResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.service.AdminService;
import com.meongnyangerang.meongnyangerang.service.ReviewReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

  private final AdminService adminService;
  private final ReviewReportService reviewReportService;

  // 관리자 로그인 API
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(new LoginResponse(adminService.login(request)));
  }

  // 호스트 가입 신청 목록 조회
  @GetMapping("/hosts/pending")
  public ResponseEntity<PageResponse<PendingHostListResponse>> getPendingHostList(
      @PageableDefault(size = 20, sort = "createdAt", direction = Direction.DESC) Pageable pageable) {

    return ResponseEntity.ok(adminService.getPendingHostList(pageable));
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

    return ResponseEntity.ok().build();
  }

  // 호스트 가입 거절
  @DeleteMapping("/hosts/{hostId}/reject")
  public ResponseEntity<Void> rejectHost(@PathVariable Long hostId) {

    adminService.rejectHost(hostId);

    return ResponseEntity.ok().build();
  }

  @GetMapping("reports/review")
  public ResponseEntity<PageResponse<ReviewReport>> getReviews(
      @PageableDefault(size = 20, sort = "createdAt", direction = Direction.DESC) Pageable pageable
  ) {
    return ResponseEntity.ok(reviewReportService.getReviews(pageable));
  }
}
