package com.meongnyangerang.meongnyangerang.dto.notification;

public record MessageNotificationRequest(
    Long chatRoomId,
    String content
) {

}
