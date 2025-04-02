package com.meongnyangerang.meongnyangerang.dto;

import java.util.List;

public record HostReviewResponse(
    List<ReviewContent> content,
    Long cursorId,
    Boolean hasNext
) {

  public static HostReviewResponse of(List<ReviewContent> content, Long cursorId, Boolean hasNext) {
    return new HostReviewResponse(content, cursorId, hasNext);
  }
}
