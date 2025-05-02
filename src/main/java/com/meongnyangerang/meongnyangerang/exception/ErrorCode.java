package com.meongnyangerang.meongnyangerang.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

  // 400 BAD REQUEST (잘못된 요청)
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
  INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
  DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "이미 가입된 이메일입니다"),
  DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "이미 등록된 닉네임입니다."),
  EXPIRED_AUTH_CODE(HttpStatus.BAD_REQUEST, "인증코드가 만료되었습니다. 다시 발급받아주세요"),
  INVALID_AUTH_CODE(HttpStatus.BAD_REQUEST, "인증코드가 일치하지 않습니다"),
  AUTH_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "인증코드를 찾을 수 없습니다. 인증코드 받기를 다시 실행해주세요"),
  USER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 등록된 회원입니다."),
  RESERVED_RESERVATION_EXISTS(HttpStatus.BAD_REQUEST, "이용 전 예약 상태가 존재합니다"),
  INVALID_FILENAME(HttpStatus.BAD_REQUEST, "파일명이 유효하지 않습니다."),
  INVALID_EXTENSION(HttpStatus.BAD_REQUEST, "파일 확장자가 유효하지 않습니다."),
  NOT_SUPPORTED_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),
  EMPTY_PET_TYPE(HttpStatus.BAD_REQUEST, "허용 반려동물 타입을 지정해 주세요."),
  RESERVATION_ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "이미 취소된 예약입니다."),
  REVIEW_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "리뷰가 이미 존재합니다."),
  MAX_IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "최대 이미지 개수는 3개입니다."),
  MISSING_IMAGE_FILE(HttpStatus.BAD_REQUEST, "파일이 비어있습니다."),
  MISSING_IMAGE_URL(HttpStatus.BAD_REQUEST, "이미지 URL이 비어있습니다."),
  MAX_PET_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "반려동물은 최대 10마리까지 등록할 수 있습니다."),
  ALREADY_REGISTERED_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "이미 등록된 전화번호입니다."),
  DUPLICATE_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "이미 사용 중인 전화번호입니다."),
  ALREADY_REGISTERED_NAME(HttpStatus.BAD_REQUEST, "이미 등록된 이름입니다."),
  ALREADY_REGISTERED_NICKNAME(HttpStatus.BAD_REQUEST, "기존 닉네임과 동일합니다."),
  EXISTS_RESERVATION(HttpStatus.BAD_REQUEST, "예약이 존재하는 객실은 삭제할 수 없습니다."),
  REVIEW_REPORT_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 신고된 리뷰입니다."),
  ROOM_COUNT_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "객실은 20개까지 생성 가능합니다."),
  SOCIAL_ACCOUNT_LOGIN_ONLY(HttpStatus.BAD_REQUEST, "소셜 계정은 일반 로그인을 지원하지 않습니다."),

  // 400 BAD REQUEST (JWT 관련 요청 오류)
  INVALID_JWT_FORMAT(HttpStatus.BAD_REQUEST, "JWT 형식이 올바르지 않습니다."),
  UNSUPPORTED_JWT(HttpStatus.BAD_REQUEST, "지원되지 않는 JWT 토큰입니다."),

  // 401 UNAUTHORIZED (인증 실패)
  EXPIRED_JWT(HttpStatus.UNAUTHORIZED, "JWT 토큰이 만료되었습니다."),
  INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다."),
  EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다."),
  INVALID_JWT_SIGNATURE(HttpStatus.UNAUTHORIZED, "JWT 서명이 유효하지 않습니다."),
  JWT_VALIDATION_ERROR(HttpStatus.UNAUTHORIZED, "JWT 검증 중 오류가 발생했습니다."),

  // 403 FORBIDDEN (접근 금지)
  INVALID_AUTHORIZED(HttpStatus.FORBIDDEN, "권한이 없습니다."),
  ACCOUNT_DELETED(HttpStatus.FORBIDDEN, "현재 계정 상태는 삭제 상태입니다."),
  ACCOUNT_PENDING(HttpStatus.FORBIDDEN, "관리자 승인 대기 중입니다."),
  REVIEW_CREATION_NOT_ALLOWED(HttpStatus.FORBIDDEN, "리뷰 작성 가능 기간이 만료되었습니다."),
  REVIEW_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "해당 리뷰에 대한 권한이 없습니다."),
  CHAT_ROOM_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "해당 채팅방에 대한 권한이 없습니다."),
  NOTIFICATION_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "알림 발송 권한이 없음"),
  WEBSOCKET_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "WebSocket 권한 없음"),

  // 404 NOT FOUND
  NOT_EXIST_ACCOUNT(HttpStatus.BAD_REQUEST, "해당 계정은 존재하지 않습니다."),
  NOT_EXISTS_HOST(HttpStatus.NOT_FOUND, "존재하지 않는 호스트입니다."),
  FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일이 존재하지 않습니다."),
  FILE_IS_EMPTY(HttpStatus.NOT_FOUND, "파일이 비어있습니다."),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저가 존재하지 않습니다."),
  ACCOMMODATION_NOT_FOUND(HttpStatus.NOT_FOUND, "숙소가 존재하지 않습니다."),
  ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "객실이 존재하지 않습니다."),
  RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약이 존재하지 않습니다."),
  REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰가 존재하지 않습니다."),
  NOT_EXIST_PET(HttpStatus.NOT_FOUND, "존재하지 않는 반려동물입니다."),
  NOT_EXIST_WISHLIST(HttpStatus.NOT_FOUND, "존재하지 않는 찜 목록입니다."),
  NOT_EXIST_NOTICE(HttpStatus.NOT_FOUND, "존재하지 않는 공지사항입니다."),
  NOT_EXIST_CHAT_ROOM(HttpStatus.NOT_FOUND, "채팅방이 존재하지 않습니다."),
  NOT_FOUND_IMAGE(HttpStatus.NOT_FOUND, "버킷에서 가져올 이미지가 없습니다."),


  // 409 Conflict
  ROOM_ALREADY_RESERVED(HttpStatus.CONFLICT, "객실이 이미 예약되었습니다."),
  CHAT_ALREADY_EXISTS(HttpStatus.CONFLICT, "채팅방이 이미 존재합니다."),


  // 409 CONFLICT
  ACCOMMODATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 개설한 숙소가 존재합니다."),
  HOST_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 승인 또는 거절된 호스트입니다."),
  ALREADY_WISHLISTED(HttpStatus.CONFLICT, "이미 찜한 숙소입니다."),

  // 500 INTERNAL SERVER ERROR (서버 내부 오류)
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다."),
  EMAIL_NOT_SEND(HttpStatus.INTERNAL_SERVER_ERROR, "이메일이 정상적으로 전송되지 않았습니다."),
  AMAZON_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "아마존 서비스 오류"),
  INVALID_IO_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 IO 오류"),
  ACCOMMODATION_REGISTRATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "숙소 등록 오류"),
  ACCOMMODATION_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "숙소 수정 오류"),
  ROOM_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "객실 수정 오류"),
  DEFAULT_RECOMMENDATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "기본 추천 조회 중 오류가 발생했습니다."),
  WEBSOCKET_SERVER_ERROR(HttpStatus.FORBIDDEN, "WebSocket 에러"),
  SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "숙소 검색 중 오류가 발생했습니다."),
  USER_RECOMMENDATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "사용자 맞춤 추천 조회 중 오류가 발생했습니다."),
  ELASTICSEARCH_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "인덱스에 문서를 저장하는 도중 오류가 발생했습니다."),
  DOCUMENT_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "해당 문서를 업데이트 하는 도중 오류가 발생했습니다.");

  private final HttpStatus status;
  private final String description;
}
