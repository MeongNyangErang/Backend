package com.meongnyangerang.meongnyangerang.dto.portone;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class TokenResponse {
  private TokenData response;

  @Getter
  public static class TokenData {
    @JsonProperty("access_token")
    private String accessToken;
  }
}

