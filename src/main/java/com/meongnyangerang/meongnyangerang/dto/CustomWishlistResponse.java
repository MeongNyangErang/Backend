package com.meongnyangerang.meongnyangerang.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomWishlistResponse<T> {
  private List<T> content;
  private Long nextCursor;
  private boolean hasNext;
}
