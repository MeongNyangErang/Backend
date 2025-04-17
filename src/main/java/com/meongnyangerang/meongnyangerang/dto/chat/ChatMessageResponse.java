package com.meongnyangerang.meongnyangerang.dto.chat;

import com.meongnyangerang.meongnyangerang.domain.chat.ChatMessage;
import com.meongnyangerang.meongnyangerang.domain.chat.MessageType;
import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import java.time.LocalDateTime;

public record ChatMessageResponse(
    Long messageId,
    String messageContent,
    SenderType senderType,
    MessageType messageType,
    LocalDateTime createdAt
) {

  public static ChatMessageResponse from(ChatMessage chatMessage) {
    return new ChatMessageResponse(
        chatMessage.getId(),
        chatMessage.getContent(),
        chatMessage.getSenderType(),
        chatMessage.getMessageType(),
        chatMessage.getCreatedAt()
    );
  }
}
