package com.meongnyangerang.meongnyangerang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HostPhoneUpdateRequest {

  @Pattern(regexp = "^01(?:0|1|[6-9])-(?:\\d{3}|\\d{4})-\\d{4}$", message = "유효하지 않은 전화번호 형식입니다")
  @NotBlank(message = "새로운 전화번호를 입력하세요.")
  private String phoneNumber;

}
