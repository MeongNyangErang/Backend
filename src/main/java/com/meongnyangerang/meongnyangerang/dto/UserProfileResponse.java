package com.meongnyangerang.meongnyangerang.dto;

import com.meongnyangerang.meongnyangerang.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UserProfileResponse {
  private String nickname;
  private String profileImageUrl;

  public static UserProfileResponse of(User user) {
    return UserProfileResponse.builder()
        .nickname(user.getNickname())
        .profileImageUrl(user.getProfileImage())
        .build();
  }
}
