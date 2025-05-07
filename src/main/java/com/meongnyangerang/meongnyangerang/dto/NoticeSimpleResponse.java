package com.meongnyangerang.meongnyangerang.dto;

import com.meongnyangerang.meongnyangerang.domain.admin.Notice;
import java.time.LocalDateTime;

public record NoticeSimpleResponse (
    Long noticeId,
    String title,
    LocalDateTime createdAt
) {

  public static NoticeSimpleResponse from(Notice notice) {
    return new NoticeSimpleResponse(
        notice.getId(),
        notice.getTitle(),
        notice.getCreatedAt()
    );
  }
}
