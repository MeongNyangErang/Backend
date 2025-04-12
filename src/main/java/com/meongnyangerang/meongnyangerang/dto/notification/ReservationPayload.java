package com.meongnyangerang.meongnyangerang.dto.notification;

import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.notification.NotificationType;
import java.time.LocalDateTime;

public record ReservationPayload(
    Long reservationId,
    String content,
    Long receiverId,
    SenderType receiverType,
    NotificationType notificationType,
    LocalDateTime createdAt
) {

  public static ReservationPayload from(
      Long reservationId,
      String content,
      Long receiverId,
      SenderType receiverType,
      NotificationType notificationType
  ) {
    return new ReservationPayload(
        reservationId,
        content,
        receiverId,
        receiverType,
        notificationType,
        LocalDateTime.now()
    );
  }
}
