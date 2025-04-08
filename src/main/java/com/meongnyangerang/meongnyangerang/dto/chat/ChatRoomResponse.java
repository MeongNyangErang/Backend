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

}
