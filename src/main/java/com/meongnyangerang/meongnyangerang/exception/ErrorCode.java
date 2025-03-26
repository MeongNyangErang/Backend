package com.meongnyangerang.meongnyangerang.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

  // 400 BAD REQUEST (잘못된 요청)
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
  DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "이미 가입된 이메일입니다"),
  EXPIRED_AUTH_CODE(HttpStatus.BAD_REQUEST, "인증코드가 만료되었습니다. 다시 발급받아주세요"),
  INVALID_AUTH_CODE(HttpStatus.BAD_REQUEST, "인증코드가 일치하지 않습니다"),
  AUTH_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "인증코드를 찾을 수 없습니다. 인증코드 받기를 다시 실행해주세요"),

  // 401 UNAUTHORIZED (인증 실패)

  // 403 FORBIDDEN (접근 금지)

  // 500 INTERNAL SERVER ERROR (서버 내부 오류)
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다."),
  EMAIL_NOT_SEND(HttpStatus.INTERNAL_SERVER_ERROR, "이메일이 정상적으로 전송되지 않았습니다.");

  private final HttpStatus status;
  private final String description;
}
