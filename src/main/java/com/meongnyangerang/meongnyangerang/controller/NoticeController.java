package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/notices")
public class NoticeController {

  private final NoticeService noticeService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Void> createNotice(
      @AuthenticationPrincipal UserDetailsImpl adminDetails,
      @RequestPart("request") @Valid NoticeRequest request,
      @RequestPart(value = "image", required = false) MultipartFile imageFile) {

    noticeService.createNotice(adminDetails.getId(), request, imageFile);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

}
