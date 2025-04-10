package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.chat.ChatRoom;
import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.notification.Notification;
import com.meongnyangerang.meongnyangerang.domain.notification.NotificationType;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.notification.NotificationReceiverInfo;
import com.meongnyangerang.meongnyangerang.dto.notification.NotificationRecord;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.NotificationRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final HostRepository hostRepository;
  private final SimpMessagingTemplate messagingTemplate;

  private static final String NOTIFICATION_DESTINATION = "/notifications";

  /**
   * 상대방에게 알림 전송
   */
  @Async
  @Transactional
  public void sendNotificationToMessagePartner(
      ChatRoom chatRoom,
      Long senderId,
      SenderType senderType,
      String content
  ) {
    log.info("비동기 알림 전송 시작");
    try {
      NotificationReceiverInfo receiverInfo = determineReceiverInfo(chatRoom, senderId, senderType);
      saveNotification(receiverInfo.user(), receiverInfo.host(), content);
      sendWebSocketNotification(
          chatRoom,
          senderId,
          senderType,
          content,
          receiverInfo.receiverId(),
          receiverInfo.receiverType()
      );
      log.info("비동기 알림 전송  성공 sender: {}, {}, receiver: {}, {}",
          senderId, senderType, receiverInfo.receiverId(), receiverInfo.receiverType());
    } catch (Exception e) {
      log.error("비동기 알림 전송 예외 발생", e);
    }
  }

  private NotificationReceiverInfo determineReceiverInfo(
      ChatRoom chatRoom,
      Long senderId,
      SenderType senderType
  ) {
    Long receiverId;
    SenderType receiverType;
    User user;
    Host host;

    if (senderType == SenderType.USER) {
      receiverId = chatRoom.getHostId();
      receiverType = SenderType.HOST;
      user = userRepository.findById(senderId)
          .orElseThrow(() -> new MeongnyangerangException(ErrorCode.USER_NOT_FOUND));
      host = hostRepository.findById(receiverId)
          .orElseThrow(() -> new MeongnyangerangException(ErrorCode.NOT_EXISTS_HOST));
    } else if (senderType == SenderType.HOST) {
      receiverId = chatRoom.getUserId();
      receiverType = SenderType.USER;
      host = hostRepository.findById(senderId)
          .orElseThrow(() -> new MeongnyangerangException(ErrorCode.NOT_EXISTS_HOST));
      user = userRepository.findById(receiverId)
          .orElseThrow(() -> new MeongnyangerangException(ErrorCode.USER_NOT_FOUND));
    } else {
      throw new MeongnyangerangException(ErrorCode.NOTIFICATION_NOT_AUTHORIZED);
    }
    return new NotificationReceiverInfo(receiverId, receiverType, user, host);
  }

  private void saveNotification(User user, Host host, String content) {
    notificationRepository.save(Notification.builder()
        .user(user)
        .host(host)
        .content(content)
        .type(NotificationType.MESSAGE)
        .isRead(false)
        .build()
    );
  }

  private void sendWebSocketNotification(
      ChatRoom chatRoom,
      Long senderId,
      SenderType senderType,
      String content,
      Long receiverId,
      SenderType receiverType
  ) {
    try {
      String userIdentifier = receiverType.name() + "_" + receiverId;
      NotificationRecord notification = new NotificationRecord(
          chatRoom.getId(),
          senderId,
          senderType,
          receiverId,
          receiverType,
          content,
          NotificationType.MESSAGE,
          LocalDateTime.now()
      );

      messagingTemplate.convertAndSendToUser(
          userIdentifier,
          NOTIFICATION_DESTINATION,
          notification
      );
      log.debug("WebSocket 알림 전송 완료 - 수신자: {}", userIdentifier);
    } catch (Exception e) {
      log.error("WebSocket 알림 전송 중 오류 발생", e);
    }
  }
}
