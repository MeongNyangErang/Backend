package com.meongnyangerang.meongnyangerang.dto;

import java.time.LocalDateTime;

public record NoticeDetailResponse (
    Long noticeId,
    String title,
    String content,
    String noticeImageUrl,
    LocalDateTime createdAt
) {}
