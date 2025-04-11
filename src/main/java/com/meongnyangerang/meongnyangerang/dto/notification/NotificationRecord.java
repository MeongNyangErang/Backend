package com.meongnyangerang.meongnyangerang.dto.notification;

import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.notification.NotificationType;
import java.time.LocalDateTime;

public record NotificationRecord(
    Long chatRoomId,
    Long senderId,
    SenderType senderType,
    Long receiverId,
    SenderType receiverType,
    String content,
    NotificationType type,
    LocalDateTime timestamp
) {

}
