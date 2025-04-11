package com.meongnyangerang.meongnyangerang.dto.notification;

import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.notification.NotificationType;
import java.time.LocalDateTime;

public record NotificationPayload(
    Long chatRoomId,
    Long senderId,
    SenderType senderType,
    Long receiverId,
    SenderType receiverType,
    String content,
    NotificationType notificationType,
    LocalDateTime createdAt
) {

  public static NotificationPayload from(
      Long chatRoomId,
      Long senderId,
      SenderType senderType,
      Long receiverId,
      SenderType receiverType,
      String content,
      NotificationType notificationType
  ) {
    return new NotificationPayload(
        chatRoomId,
        senderId,
        senderType,
        receiverId,
        receiverType,
        content,
        notificationType,
        LocalDateTime.now()
    );
  }
}
