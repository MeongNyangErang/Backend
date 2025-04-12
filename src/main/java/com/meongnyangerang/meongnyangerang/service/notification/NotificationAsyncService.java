package com.meongnyangerang.meongnyangerang.service.notification;

import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.notification.NotificationType;
import com.meongnyangerang.meongnyangerang.dto.notification.NotificationPayload;
import com.meongnyangerang.meongnyangerang.dto.notification.ReservationPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationAsyncService {

  private final SimpMessagingTemplate messagingTemplate;

  private static final String NOTIFICATION_DESTINATION = "/subscribe/notifications";

  @Async
  public void sendMessageNotification(
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

      messagingTemplate.convertAndSendToUser(
          receiverKey,
          NOTIFICATION_DESTINATION,
          payload
      );
      log.debug("WebSocket 비동기 메시지 알림 전송 완료 - 수신자: {}", receiverKey);
    } catch (Exception e) {
      log.error("WebSocket 비동기 메시지 알림 전송 중 오류 발생", e);
    }
  }

  @Async
  public void sendReservationNotification(
      Long reservationId,
      String content,
      Long receiverId,
      SenderType receiverType,
      NotificationType notificationType
  ) {
    try {
      ReservationPayload payload = ReservationPayload.from(
          reservationId, content,
          receiverId, receiverType,
          notificationType
      );
      String receiverKey = receiverType.name() + "_" + receiverId;

      messagingTemplate.convertAndSendToUser(
          receiverKey,
          NOTIFICATION_DESTINATION,
          payload
      );
      log.debug("WebSocket 비동기 예약 알림 전송 완료 - 수신자: {}", receiverKey);
    } catch (Exception e) {
      log.error("WebSocket 비동기 예약 알림 전송 중 오류 발생", e);
    }
  }
}
