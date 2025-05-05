package com.meongnyangerang.meongnyangerang.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KakaoUserInfoResponse {
  private Long id;

  @JsonProperty("kakao_account")
  private KakaoAccount kakaoAccount;

  @Getter
  @Setter
  public static class KakaoAccount {
    private String email;
    private Profile profile;

    @Getter
    @Setter
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
