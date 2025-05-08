package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.NoticeSimpleResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notices")
public class PublicNoticeController {

  private final NoticeService noticeService;

  // 공지사항 목록 조회 API
  @GetMapping
  public ResponseEntity<PageResponse<NoticeSimpleResponse>> getNoticeList(
      @PageableDefault(size = 20, sort = "createdAt", direction = Direction.DESC) Pageable pageable) {

    return ResponseEntity.ok(noticeService.getNoticeList(pageable));
  }

  // 공지사항 상세 조회 API
  @GetMapping("/{noticeId}")
  public ResponseEntity<NoticeDetailResponse> getNoticeDetail(@PathVariable Long noticeId) {
    return ResponseEntity.ok(noticeService.getNoticeDetail);
  }
}
