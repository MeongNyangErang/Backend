package com.meongnyangerang.meongnyangerang.dto.notification;

import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.notification.NotificationType;
import java.time.LocalDateTime;

public record ReservationNotificationPayload(
    Long notificationId,
    Long reservationId,
    String content,
    Long receiverId,
    SenderType receiverType,
    NotificationType notificationType,
    LocalDateTime createdAt
) {

  public static ReservationNotificationPayload from(
      Long notificationId,
      Long reservationId,
      String content,
      Long receiverId,
      SenderType receiverType,
      NotificationType notificationType
  ) {
    return new ReservationNotificationPayload(
        notificationId,
        reservationId,
        content,
        receiverId,
        receiverType,
        notificationType,
        LocalDateTime.now()
    );
  }
}
