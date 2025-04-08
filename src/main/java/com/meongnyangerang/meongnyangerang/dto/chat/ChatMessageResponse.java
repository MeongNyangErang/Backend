package com.meongnyangerang.meongnyangerang.dto.chat;

import com.meongnyangerang.meongnyangerang.domain.chat.ChatMessage;
import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import java.time.LocalDateTime;

public record ChatMessageResponse(
    Long messageId,
    SenderType senderType,
    String content,
    LocalDateTime createdAt
) {

  public static ChatMessageResponse from(ChatMessage chatMessage) {
    return new ChatMessageResponse(
        chatMessage.getId(),
        chatMessage.getSenderType(),
        chatMessage.getContent(),
        chatMessage.getCreatedAt()
    );
  }
}
