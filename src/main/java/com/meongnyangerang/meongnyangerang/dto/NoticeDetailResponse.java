package com.meongnyangerang.meongnyangerang.dto;

import com.meongnyangerang.meongnyangerang.domain.admin.Notice;
import java.time.LocalDateTime;

public record NoticeDetailResponse (
    Long noticeId,
    String title,
    String content,
    String noticeImageUrl,
    LocalDateTime createdAt
) {

  public static NoticeDetailResponse from(Notice notice) {
    return new NoticeDetailResponse(
        notice.getId(),
        notice.getTitle(),
        notice.getContent(),
        notice.getImageUrl(),
        notice.getCreatedAt()
    );
  }
}
