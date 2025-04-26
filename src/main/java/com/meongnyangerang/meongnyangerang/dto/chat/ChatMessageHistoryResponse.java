package com.meongnyangerang.meongnyangerang.dto.chat;

public record ChatMessageHistoryResponse(
    PageResponse<ChatMessageResponse> chatMessagePage,
    String partnerName,
    String partnerImageUrl
) {

  public static ChatMessageHistoryResponse of(
      PageResponse<ChatMessageResponse> chatMessagePage,
      String partnerName,
      String partnerImageUrl
  ) {
    return new ChatMessageHistoryResponse(chatMessagePage, partnerName, partnerImageUrl);
  }
}
