package com.meongnyangerang.meongnyangerang.dto.notification;

import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.notification.NotificationType;
import java.time.LocalDateTime;

public record ReviewNotificationPayload(
    Long notificationId,
    Long reviewId,
    String content,
    Long receiverId,
    SenderType receiverType,
    NotificationType notificationType,
    LocalDateTime createdAt
) {

  public static ReviewNotificationPayload from(
      Long notificationId,
      Long reviewId,
      String content,
      Long receiverId,
      SenderType receiverType
  ) {
    return new ReviewNotificationPayload(
        notificationId,
        reviewId,
        content,
        receiverId,
        receiverType,
        NotificationType.REVIEW,
        LocalDateTime.now()
    );
  }
}