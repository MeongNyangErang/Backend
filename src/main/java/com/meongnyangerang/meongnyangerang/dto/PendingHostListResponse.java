package com.meongnyangerang.meongnyangerang.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PendingHostListResponse {

  private Long hostId;
  private String createdAt;

}
