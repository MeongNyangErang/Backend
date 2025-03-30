package com.meongnyangerang.meongnyangerang.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomReviewResponse<T> {

  private List<T> content;
  private Long cursor;
  private boolean hasNext;
}
