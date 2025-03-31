package com.meongnyangerang.meongnyangerang.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomApplicationResponse {

  private List<PendingHostListResponse> content;
  private Long cursor;
  private boolean hasNext;
}
