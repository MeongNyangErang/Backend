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

  @Async
  @Transactional
  public void sendGenericNotification(NotificationRecord request) {
    User user = userRepository.findById(
        request.receiverType() == SenderType.USER ? request.receiverId() : request.senderId())
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.USER_NOT_FOUND));
    Host host = hostRepository.findById(
        request.receiverType() == SenderType.HOST ? request.receiverId() : request.senderId())
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.NOT_EXISTS_HOST));

    // 알림 DB 저장
    notificationRepository.save(Notification.builder()
        .user(user)
        .host(host)
        .content(request.content())
        .type(request.notificationType())
        .isRead(false)
        .build());

    // WebSocket 알림 전송
    String receiverKey = request.receiverType().name() + "_" + request.receiverId();
    messagingTemplate.convertAndSendToUser(receiverKey, "/notifications",
        new NotificationRecord(
            request.chatRoomId(),
            request.senderId(),
            request.senderType(),
            request.receiverId(),
            request.receiverType(),
            request.content(),
            request.notificationType(),
            LocalDateTime.now()
        )
    );
  }

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
    try {
      NotificationReceiverInfo receiverInfo = determineReceiverInfo(chatRoom, senderId, senderType);
      saveNotification(receiverInfo.user(), receiverInfo.host(), content);
      sendWebSocketNotification(
          receiverInfo.receiverEmail(),
          chatRoom,
          senderId,
          senderType,
          content,
          receiverInfo.receiverId(),
          receiverInfo.receiverType()
      );
    } catch (Exception e) {
      log.error("비동기 알림 전송 예외 발생", e);
    }
  }

  private NotificationReceiverInfo determineReceiverInfo(
      ChatRoom chatRoom,
      Long senderId,
      SenderType senderType
  ) {
    if (senderType == SenderType.USER) {
      return determineHostReceiverInfo(chatRoom, senderId);
    } else if (senderType == SenderType.HOST) {
      return determineUserReceiverInfo(chatRoom, senderId);
    } else {
      throw new MeongnyangerangException(ErrorCode.NOTIFICATION_NOT_AUTHORIZED);
    }
  }

  private NotificationReceiverInfo determineHostReceiverInfo(ChatRoom chatRoom, Long userId) {
    Long hostId = chatRoom.getHostId();
    User sender = findUserById(userId);
    Host receiver = findHostById(hostId);

    return new NotificationReceiverInfo(
        hostId,
        SenderType.HOST,
        sender,
        receiver,
        receiver.getEmail()
    );
  }

  private NotificationReceiverInfo determineUserReceiverInfo(ChatRoom chatRoom, Long hostId) {
    Long userId = chatRoom.getUserId();
    Host sender = findHostById(hostId);
    User receiver = findUserById(userId);

    return new NotificationReceiverInfo(
        userId,
        SenderType.USER,
        receiver,
        sender,
        receiver.getEmail()
    );
  }

  private User findUserById(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.USER_NOT_FOUND));
  }

  private Host findHostById(Long hostId) {
    return hostRepository.findById(hostId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.NOT_EXISTS_HOST));
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
      String receiverName,
      ChatRoom chatRoom,
      Long senderId,
      SenderType senderType,
      String content,
      Long receiverId,
      SenderType receiverType
  ) {
    try {
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
          receiverName,
          NOTIFICATION_DESTINATION,
          notification
      );
      log.info("WebSocket 알림 전송 완료 - 수신자: {}", receiverName);
    } catch (Exception e) {
      log.error("WebSocket 알림 전송 중 오류 발생", e);
    }
  }
}
