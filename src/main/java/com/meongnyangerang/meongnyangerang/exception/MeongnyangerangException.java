package com.meongnyangerang.meongnyangerang.exception;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@NoArgsConstructor
@Builder
public class MeongnyangerangException extends RuntimeException {

  private ErrorCode errorCode;

  public MeongnyangerangException(ErrorCode errorCode) {
    super(errorCode.getDescription()); // 부모 생성자에 메시지 전달(RuntimeException)
    this.errorCode = errorCode;
  }

  public HttpStatus getHttpStatus() {
    return errorCode.getStatus(); // ErrorCode 에서 HttpStatus 추출
  }
}
