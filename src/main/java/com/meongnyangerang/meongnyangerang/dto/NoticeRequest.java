package com.meongnyangerang.meongnyangerang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoticeRequest {

  @NotBlank(message = "제목은 필수입니다.")
  @Size(max = 100, message = "제목은 최대 100자까지 입력할 수 있습니다.")
  private String title;

  @NotBlank(message = "내용은 필수입니다.")
  @Size(max = 5000, message = "내용은 최대 5000자까지 입력할 수 있습니다.")
  private String content;
}
