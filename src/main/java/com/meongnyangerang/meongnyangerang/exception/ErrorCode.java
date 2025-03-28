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
  DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "이미 등록된 닉네임입니다."),
  EXPIRED_AUTH_CODE(HttpStatus.BAD_REQUEST, "인증코드가 만료되었습니다. 다시 발급받아주세요"),
  INVALID_AUTH_CODE(HttpStatus.BAD_REQUEST, "인증코드가 일치하지 않습니다"),
  AUTH_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "인증코드를 찾을 수 없습니다. 인증코드 받기를 다시 실행해주세요"),
  USER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 등록된 회원입니다."),
  INVALID_FILENAME(HttpStatus.BAD_REQUEST, "파일명이 유효하지 않습니다."),
  INVALID_EXTENSION(HttpStatus.BAD_REQUEST, "파일 확장자가 유효하지 않습니다."),
  NOT_SUPPORTED_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),
  EMPTY_PET_TYPE(HttpStatus.BAD_REQUEST, "허용 반려동물 타입을 지정해 주세요."),

  // 401 UNAUTHORIZED (인증 실패)

  // 403 FORBIDDEN (접근 금지)
  INVALID_AUTHORIZED(HttpStatus.FORBIDDEN, "권한이 없습니다."),

  // 404  NOT_FOUND
  NOT_EXISTS_HOST(HttpStatus.NOT_FOUND, "존재하지 않는 호스트입니다."),
  FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일이 존재하지 않습니다."),
  FILE_NOT_EMPTY(HttpStatus.NOT_FOUND, "파일이 비어있습니다."),

  // 409 CONFLICT
  ACCOMMODATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 개설한 숙소가 존재합니다."),

  // 500 INTERNAL SERVER ERROR (서버 내부 오류)
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다."),
  EMAIL_NOT_SEND(HttpStatus.INTERNAL_SERVER_ERROR, "이메일이 정상적으로 전송되지 않았습니다."),
  AMAZON_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "아마존 서비스 오류"),
  INVALID_IO_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 IO 오류"),
  ;

  private final HttpStatus status;
  private final String description;
}
