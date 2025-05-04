package com.meongnyangerang.meongnyangerang.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class KakaoUserInfoResponse {
  private Long id;

  @JsonProperty("kakao_account")
  private KakaoAccount kakaoAccount;

  @Getter
  public static class KakaoAccount {
    private String email;
    private Profile profile;

    @Getter
    public static class Profile {
      private String nickname;

      @JsonProperty("profile_image_url")
      private String profileImageUrl;
    }
  }

  public String email() {
    return kakaoAccount != null ? kakaoAccount.getEmail() : null;
  }

  public String nickname() {
    return kakaoAccount != null && kakaoAccount.getProfile() != null
        ? kakaoAccount.getProfile().getNickname() : null;
  }

  public String profileImage() {
    return kakaoAccount != null && kakaoAccount.getProfile() != null
        ? kakaoAccount.getProfile().getProfileImageUrl() : null;
  }
}
