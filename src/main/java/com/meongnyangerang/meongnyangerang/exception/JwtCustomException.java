package com.meongnyangerang.meongnyangerang.exception;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class JwtCustomException extends RuntimeException {

  private ErrorCode errorCode;

  public JwtCustomException(ErrorCode errorCode) {
    super(errorCode.getDescription()); // 부모 생성자에 메시지 전달
    this.errorCode = errorCode;
  }

  public HttpStatus getHttpStatus() {
    return errorCode.getStatus(); // ErrorCode에서 HttpStatus 가져오기
  }
}
