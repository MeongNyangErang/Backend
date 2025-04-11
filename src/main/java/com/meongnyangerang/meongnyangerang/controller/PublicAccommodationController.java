package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationDetailResponse;
import com.meongnyangerang.meongnyangerang.service.AccommodationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
      @PathVariable Long accommodationId) {

    return ResponseEntity.ok(accommodationService.getAccommodationDetail(accommodationId));
  }
}
