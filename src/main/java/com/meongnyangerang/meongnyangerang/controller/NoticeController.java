package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.NoticeRequest;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/notices")
public class NoticeController {

  private final NoticeService noticeService;

  // 공지사항 등록 API
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Void> createNotice(
      @AuthenticationPrincipal UserDetailsImpl adminDetails,
      @RequestPart("request") @Valid NoticeRequest request,
      @RequestPart(value = "image", required = false) MultipartFile imageFile) {

    noticeService.createNotice(adminDetails.getId(), request, imageFile);
    return ResponseEntity.ok().build();
  }

  // 공지사항 수정 API
  @PutMapping("/{noticeId}")
  public ResponseEntity<Void> updateNotice(
      @AuthenticationPrincipal UserDetailsImpl adminDetails,
      @PathVariable Long noticeId,
      @RequestPart("request") @Valid NoticeRequest request,
      @RequestPart(value = "image", required = false) MultipartFile imageFile) {

    noticeService.updateNotice(adminDetails.getId(), noticeId, request, imageFile);
    return ResponseEntity.ok().build();
  }

  // 공지사항 삭제 API
  @DeleteMapping("/{noticeId}")
  public ResponseEntity<Void> deleteNotice(
      @AuthenticationPrincipal UserDetailsImpl adminDetails,
      @PathVariable Long noticeId) {

    noticeService.deleteNotice(adminDetails.getId(), noticeId);
    return ResponseEntity.ok().build();
  }
}
