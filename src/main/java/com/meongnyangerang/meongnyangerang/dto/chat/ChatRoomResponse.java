package com.meongnyangerang.meongnyangerang.dto.chat;

import java.time.LocalDateTime;

public record ChatRoomResponse(
    Long chatRoomId,
    Long partnerId,
    String partnerName,
    String partnerImageUrl,
    String lastMessage,
    LocalDateTime lastMessageTime,
    int unreadCount
) {

  public static ChatRoomResponse createChatRoomResponse(
      Long chatRoomId,
      Long partnerId,
      String partnerName,
      String partnerImageUrl,
      String lastMessage,
      LocalDateTime lastMessageTime,
      int unreadCount
  ) {
    return new ChatRoomResponse(
        chatRoomId,
        partnerId,       // 상대방 ID
        partnerName,     // 상대방 이름 (호스트는 숙소 이름)
        partnerImageUrl, // 상대방 프로필 이미지 (호스트는 숙소 썸네일)
        lastMessage != null ? lastMessage : "",
        lastMessageTime,
        unreadCount
    );
  }
}
