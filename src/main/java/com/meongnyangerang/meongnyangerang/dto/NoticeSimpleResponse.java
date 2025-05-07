package com.meongnyangerang.meongnyangerang.dto;

import java.time.LocalDateTime;

public record NoticeSimpleResponse (
    Long noticeId,
    String title,
    LocalDateTime createdAt
) {}
