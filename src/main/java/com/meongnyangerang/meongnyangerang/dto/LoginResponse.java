package com.meongnyangerang.meongnyangerang.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public class LoginResponse {

  private String accessToken;
  private String refreshToken;
}
