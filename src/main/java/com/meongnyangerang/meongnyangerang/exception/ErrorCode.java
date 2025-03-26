package com.meongnyangerang.meongnyangerang.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

  // 400 BAD REQUEST (잘못된 요청)

  // 401 UNAUTHORIZED (인증 실패)

  // 403 FORBIDDEN (접근 금지)

  // 500 INTERNAL SERVER ERROR (서버 내부 오류)
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다."),
  EMAIL_NOT_SEND(HttpStatus.INTERNAL_SERVER_ERROR, "이메일이 정상적으로 전송되지 않았습니다.");

  private final HttpStatus status;
  private final String description;
}
