package com.meongnyangerang.meongnyangerang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NicknameRequest {

  @Size(min = 2, max = 20)
  @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
  private String nickname;

}
