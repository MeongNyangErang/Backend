package com.meongnyangerang.meongnyangerang.dto.chat;

import com.meongnyangerang.meongnyangerang.domain.chat.MessageType;
import java.time.LocalDateTime;

public record ChatRoomResponse(
    Long chatRoomId,
    Long partnerId,
    String partnerName,
    String partnerImageUrl,
    String lastMessage,
    MessageType lastMessageType,
    LocalDateTime lastMessageTime,
    int unreadCount
) {

}
