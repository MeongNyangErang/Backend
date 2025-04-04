package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationCreateRequest;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationResponse;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationUpdateRequest;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.AccommodationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/hosts/accommodations")
public class AccommodationController {

  private final AccommodationService accommodationService;

  /**
   * 숙소 등록 API 숙소 정보와 이미지 파일을 함께 등록
   */
  @PostMapping
  public ResponseEntity<Void> createAccommodation(
      @AuthenticationPrincipal UserDetailsImpl userDetail,
      @Valid @RequestPart AccommodationCreateRequest request,
      @RequestPart MultipartFile thumbnail,
      @RequestPart(required = false) List<MultipartFile> additionalImages
  ) {
    accommodationService.createAccommodation(
        userDetail.getId(), request, thumbnail, additionalImages);
    return ResponseEntity.ok().build();
  }

  /**
   * 숙소 조회 API
   */
  @GetMapping
  public ResponseEntity<AccommodationResponse> getAccommodation(
      @AuthenticationPrincipal UserDetailsImpl userDetail
  ) {
    return ResponseEntity.ok(accommodationService.getAccommodation(userDetail.getId()));
  }

  /**
   * 숙소 수정 API
   */
  @PutMapping
  public ResponseEntity<AccommodationResponse> updateAccommodation(
      @AuthenticationPrincipal UserDetailsImpl userDetail,
      @Valid @RequestPart AccommodationUpdateRequest request,
      @RequestPart(required = false) MultipartFile newThumbnail,
      @RequestPart(required = false) List<MultipartFile> newAdditionalImages
  ) {
    return ResponseEntity.ok(
        accommodationService.updateAccommodation(
            userDetail.getId(), request, newThumbnail, newAdditionalImages));
  }
}
