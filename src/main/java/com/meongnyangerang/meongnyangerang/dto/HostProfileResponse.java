package com.meongnyangerang.meongnyangerang.dto;

import com.meongnyangerang.meongnyangerang.domain.host.Host;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class HostProfileResponse {

  private String name;
  private String nickname;
  private String phone;
  private String profileImageUrl;

  public static HostProfileResponse of(Host host) {
    return HostProfileResponse.builder()
        .name(host.getName())
        .nickname(host.getNickname())
        .phone(host.getPhoneNumber())
        .profileImageUrl(host.getProfileImageUrl())
        .build();
  }
}
