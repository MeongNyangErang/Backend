package com.meongnyangerang.meongnyangerang.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyCodeRequest {

  @Email(message = "유효한 이메일 주소를 입력해주세요.")
  @NotBlank(message = "이메일은 필수 입력 항목입니다.")
  private String email;

  @NotBlank(message = "인증 코드는 필수 입력 항목입니다.")
  @Pattern(regexp = "^\\d{6}$", message = "인증 코드는 6자리 숫자여야 합니다.")
  private String code;
}
