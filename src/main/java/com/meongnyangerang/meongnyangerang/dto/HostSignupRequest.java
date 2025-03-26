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
public class HostSignupRequest {

  @Email(message = "유효한 이메일 주소를 입력해주세요.")
  @NotBlank(message = "이메일은 필수 입력 항목입니다.")
  private String email;


  private String name;

  @Size(min = 2, max = 20)
  @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
  private String nickname;

  @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$") // 최소 8자, 영문+숫자
  @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
  private String password;

  private String profileImageUrl;

  @NotBlank(message = "사업자 등록증 이미지는 필수입니다")
  private String businessLicenseImageUrl;

  @NotBlank(message = "숙박업 허가증 이미지는 필수입니다")
  private String submitDocumentImageUrl;

  @Pattern(regexp = "^01(?:0|1|[6-9])-(?:\\d{3}|\\d{4})-\\d{4}$", message = "유효하지 않은 전화번호 형식입니다")
  private String phoneNumber;

}

