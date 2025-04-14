package com.meongnyangerang.meongnyangerang.dto.notification;

import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.notification.NotificationType;
import java.time.LocalDateTime;

public record NotificationPayload(
    Long id,
    String content,
    Long receiverId,
    SenderType receiverType,
    NotificationType notificationType,
    LocalDateTime createdAt
) {

  public static NotificationPayload from(
      Long id,
      String content,
      Long receiverId,
      SenderType receiverType,
      NotificationType notificationType
  ) {
    return new NotificationPayload(
        id,
        content,
        receiverId,
        receiverType,
        notificationType,
        LocalDateTime.now()
    );
  }
}
