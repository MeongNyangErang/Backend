package com.meongnyangerang.meongnyangerang.dto.notification;

import com.meongnyangerang.meongnyangerang.domain.notification.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(
    Long notificationId,
    String content,
    NotificationType notificationType,
    Boolean isRead,
    LocalDateTime createdAt
) {

}
