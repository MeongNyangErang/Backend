package com.meongnyangerang.meongnyangerang.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class KakaoTokenResponse {
  @JsonProperty("access_token")
  private String accessToken;
}
