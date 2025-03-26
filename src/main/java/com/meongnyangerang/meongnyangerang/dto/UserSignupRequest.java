package com.meongnyangerang.meongnyangerang.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSignupRequest {

  @Email(message = "유효한 이메일 주소를 입력해주세요.")
  @NotBlank(message = "이메일은 필수 입력 항목입니다.")
  private String email;

  @Size(min = 2, max = 20)
  @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
  private String nickname;

  @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$") // 최소 8자, 영문+숫자
  @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
  private String password;

  private String profileImage;

}
