package com.meongnyangerang.meongnyangerang.controller;

import static com.meongnyangerang.meongnyangerang.domain.user.Role.*;

import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationDetailResponse;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.AccommodationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/accommodations")
public class PublicAccommodationController {

  private final AccommodationService accommodationService;

  // 숙소 상세 조회 API (모든 유저가 사용 가능)
  @GetMapping("/{accommodationId}")
  public ResponseEntity<AccommodationDetailResponse> getAccommodationDetail(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @PathVariable Long accommodationId) {

    Long userId = null;

    // 로그인한 사용자이면서 일반 사용자(ROLE_USER)일 때만 userId 사용
    if (userDetails != null && userDetails.getRole() == ROLE_USER) {
      userId = userDetails.getId();
    }

    return ResponseEntity.ok(accommodationService.getAccommodationDetail(accommodationId, userId));
  }
}
