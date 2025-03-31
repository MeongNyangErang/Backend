package com.meongnyangerang.meongnyangerang.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PendingHostDetailResponse {

  private String email;
  private String name;
  private String phoneNumber;
  private String businessLicenseImageUrl;
  private String submitDocumentImageUrl;

}
