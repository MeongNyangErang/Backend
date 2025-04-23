package com.meongnyangerang.meongnyangerang.dto.chat;

import java.util.List;
import org.springframework.data.domain.Page;

public record ChatMessagePageResponse<T>(
    List<T> content,
    String receiverName,
    String receiverImageUrl,
    int page,           // 현재 페이지 번호
    int size,           // 페이지당 항목 수
    long totalElements, // 전체 데이터 항목 수
    int totalPages,     // 전체 페이지 수
    boolean first,      // 현재 페이지가 첫 번째 페이지인지 여부
    boolean last        // 현재 페이지가 마지막 페이지인지 여부
) {

  public static <T> ChatMessagePageResponse<T> from(
      Page<T> page,
      String receiverName,
      String receiverImageUrl
  ) {
    return new ChatMessagePageResponse<>(
        page.getContent(),
        receiverName,
        receiverImageUrl,
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isFirst(),
        page.isLast()
    );
  }
}
