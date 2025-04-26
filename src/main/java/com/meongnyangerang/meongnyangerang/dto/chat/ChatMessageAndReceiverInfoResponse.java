package com.meongnyangerang.meongnyangerang.dto.chat;

import com.meongnyangerang.meongnyangerang.domain.chat.ChatMessage;
import com.meongnyangerang.meongnyangerang.domain.chat.MessageType;
import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import java.time.LocalDateTime;

public record ChatMessageAndReceiverInfoResponse(
    Long chatRoomId,
    String receiverName,
    String receiverImageUrl,
    String messageContent,
    SenderType senderType,
    MessageType messageType,
    LocalDateTime createdAt
) {

  public static ChatMessageAndReceiverInfoResponse from(
      ChatMessage chatMessage,
      String receiverName,
      String receiverImageUrl
  ) {
    return new ChatMessageAndReceiverInfoResponse(
        chatMessage.getChatRoom().getId(),
        receiverName,
        receiverImageUrl,
        chatMessage.getContent(),
        chatMessage.getSenderType(),
        chatMessage.getMessageType(),
        chatMessage.getCreatedAt()
    );
  }
}
