package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationCreateRequest;
import com.meongnyangerang.meongnyangerang.service.AccommodationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/accommodations")
public class AccommodationController {

  private final AccommodationService accommodationService;

  /**
   * 숙소 등록 API 숙소 정보와 이미지 파일을 함께 등록
   *
   * @param request 숙소 등록 요청 정보
   * @param additionalImages  숙소 이미지 파일 목록
   * @return 201
   */
  // TODO: UserDetails 구현이 완료되면 주석 해제
  // TODO: host만 호출할 수 있도록 수정
  @PostMapping
  public ResponseEntity<Void> createAccommodation(
      //@AuthenticationPrincipal UserDetail userDetail
      @Valid @RequestPart AccommodationCreateRequest request,
      @RequestPart MultipartFile thumbnail,
      @RequestPart(required = false) List<MultipartFile> additionalImages
  ) {
    accommodationService.createAccommodation(1L, request, thumbnail, additionalImages);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}
