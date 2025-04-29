package com.meongnyangerang.meongnyangerang.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
    @NotBlank(message = "리프레시 토큰이 누락되었습니다.")
    String refreshToken
) {}
