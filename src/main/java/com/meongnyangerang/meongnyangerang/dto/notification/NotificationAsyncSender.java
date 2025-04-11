package com.meongnyangerang.meongnyangerang.dto.notification;

import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationAsyncSender {

  private final SimpMessagingTemplate messagingTemplate;

  private static final String NOTIFICATION_DESTINATION = "/subscribe/notifications";

  @Async
  public void sendWebSocketNotification(
      Long chatRoomId,
      Long senderId,
      SenderType senderType,
      String content,
      Long receiverId,
      SenderType receiverType,
      NotificationType notificationType
  ) {
    try {
      NotificationPayload payload = NotificationPayload.from(
          chatRoomId, senderId,
          senderType, receiverId,
          receiverType, content,
          notificationType
      );
      String receiverKey = receiverType.name() + "_" + receiverId;

      log.info("알림 전송: {}", receiverKey + NOTIFICATION_DESTINATION);

      messagingTemplate.convertAndSendToUser(
          receiverKey,
          NOTIFICATION_DESTINATION,
          payload
      );
      log.info("WebSocket 알림 전송 완료 - 수신자: {}", receiverKey);
    } catch (Exception e) {
      log.error("WebSocket 알림 전송 중 오류 발생", e);
    }
  }
}
