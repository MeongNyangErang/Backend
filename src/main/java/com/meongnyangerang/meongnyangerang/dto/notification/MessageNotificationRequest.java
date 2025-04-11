package com.meongnyangerang.meongnyangerang.dto.notification;

import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;

public record MessageNotificationRequest(
    Long chatRoomId,
    SenderType senderType,
    String content
) {

}
