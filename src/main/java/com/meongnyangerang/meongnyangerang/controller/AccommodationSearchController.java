package com.meongnyangerang.meongnyangerang.controller;

import static com.meongnyangerang.meongnyangerang.domain.user.Role.ROLE_USER;

import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationSearchRequest;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationSearchResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.AccommodationSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search/accommodations")
public class AccommodationSearchController {

  private final AccommodationSearchService searchService;

  @PostMapping
  public ResponseEntity<PageResponse<AccommodationSearchResponse>> searchAccommodation(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @Valid @RequestBody AccommodationSearchRequest request,
      @PageableDefault(size = 20) Pageable pageable) {

    Long userId = null;

    // 로그인한 사용자이면서 일반 사용자(ROLE_USER)일 때만 userId 사용
    if (userDetails != null && userDetails.getRole() == ROLE_USER) {
      userId = userDetails.getId();
    }

    return ResponseEntity.ok(searchService.searchAccommodation(userId, request, pageable));
  }
}
