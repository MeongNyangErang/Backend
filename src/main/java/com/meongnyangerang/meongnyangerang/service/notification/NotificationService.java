package com.meongnyangerang.meongnyangerang.service.notification;

import com.meongnyangerang.meongnyangerang.domain.chat.ChatRoom;
import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.notification.Notification;
import com.meongnyangerang.meongnyangerang.domain.notification.NotificationType;
import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
import com.meongnyangerang.meongnyangerang.domain.review.Review;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.dto.notification.NotificationReceiverInfo;
import com.meongnyangerang.meongnyangerang.dto.notification.MessageNotificationRequest;
import com.meongnyangerang.meongnyangerang.dto.notification.NotificationResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.NotificationRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final UserRepository userRepository;
  private final HostRepository hostRepository;
  private final NotificationAsyncService notificationAsyncSender;

  private static final String RESERVATION_REMIND_CONTENT = "%s 숙소 체크인이 내일입니다. 체크인 시간은 %s입니다.";
  private static final String WRITE_REVIEW_CONTENT = "%s 님이 리뷰를 남겼습니다.";

  /**
   * 상대방에게 메시지 알림 전송
   */
  public void sendMessageNotification(
      MessageNotificationRequest request,
      Long senderId,
      SenderType senderType
  ) {
    ChatRoom chatRoom = findAndValidateChatRoom(request.chatRoomId(), senderId, senderType);
    sendNotificationToMessagePartner(chatRoom, senderId, senderType, request.content());
  }

  /**
   * 예약 알림 전송
   */
  @Transactional
  public void sendReservationNotification(
      Long reservationId,
      User user,
      Host host,
      String contentToUser,
      String contentToHost
  ) {
    // 사용자에게 예약 확정 알림 전송 및 저장
    sendReservationNotificationToUser(reservationId, user, contentToUser);
    // 호스트에게 예약 등록 알림 전송 및 저장
    sendReservationNotificationToHost(reservationId, host, contentToHost);
  }

  /**
   * 사용자 알림 목록 조회
   */
  @Transactional
  public PageResponse<NotificationResponse> getNotificationsAsUser(Long userId, Pageable pageable) {
    Page<Notification> notifications = notificationRepository.findAllByUser_IdOrderByCreatedAtDesc(
        userId, pageable);
    notifications.forEach(Notification::makeAsRead); // 읽음상태를 모두 true로 변경
    Page<NotificationResponse> response = notifications.map(this::createNotificationResponse);

    return PageResponse.from(response);
  }

  /**
   * 호스트 알림 목록 조회
   */
  @Transactional
  public PageResponse<NotificationResponse> getNotificationsAsHost(Long userId, Pageable pageable) {
    Page<Notification> notifications = notificationRepository.findAllByHost_IdOrderByCreatedAtDesc(
        userId, pageable);
    notifications.forEach(Notification::makeAsRead); // 읽음여부를 모두 true로 변경
    Page<NotificationResponse> response = notifications.map(this::createNotificationResponse);

    return PageResponse.from(response);
  }

  /**
   * 일반회원 알림 삭제
   */
  @Transactional
  public void deleteNotificationAsUser(Long notificationId, Long userId) {
    notificationRepository.deleteByIdAndUser_Id(notificationId, userId);
  }

  /**
   * 호스트 알림 삭제
   */
  @Transactional
  public void deleteNotificationAsHost(Long notificationId, Long hostId) {
    notificationRepository.deleteByIdAndHost_Id(notificationId, hostId);
  }

  /**
   * 예약 리마인드 알림 발송
   */
  public void sendReservationReminderNotification(Reservation reservation) {
    User user = reservation.getUser();
    String content = String.format(RESERVATION_REMIND_CONTENT,
        reservation.getAccommodationName(), reservation.getCheckInDate());

    Notification savedNotification = saveNotificationAsUser(
        user, content, NotificationType.RESERVATION_REMINDER);
    notificationAsyncSender.sendReservationNotification(
        savedNotification.getId(),
        reservation.getId(),
        content,
        user.getId(),
        SenderType.USER,
        NotificationType.RESERVATION_REMINDER
    );
  }

  /**
   * 리뷰 작성 알림 발송 to Host
   */
  public void sendReviewNotification(Review review) {
    User user = review.getUser();
    Host host = review.getAccommodation().getHost();

    String content = String.format(WRITE_REVIEW_CONTENT, user.getNickname());
    Notification savedNotification = saveNotificationAsHost(host, content, NotificationType.REVIEW);

    notificationAsyncSender.sendReviewNotification(
        savedNotification.getId(),
        review.getId(),
        content,
        host.getId()
    );
  }

  private void sendReservationNotificationToUser(Long reservationId, User user, String content) {
    Notification savedNotification = saveNotificationAsUser(
        user, content, NotificationType.RESERVATION_CONFIRMED);

    notificationAsyncSender.sendReservationNotification(
        savedNotification.getId(),
        reservationId,
        content,
        user.getId(),
        SenderType.USER,
        NotificationType.RESERVATION_CONFIRMED
    );
  }

  private void sendReservationNotificationToHost(Long reservationId, Host host, String content) {
    Notification savedNotification = saveNotificationAsHost(
        host, content, NotificationType.RESERVATION_REGISTERED);

    notificationAsyncSender.sendReservationNotification(
        savedNotification.getId(),
        reservationId,
        content,
        host.getId(),
        SenderType.HOST,
        NotificationType.RESERVATION_REGISTERED
    );
  }

  private NotificationResponse createNotificationResponse(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getContent(),
        notification.getType(),
        notification.getCreatedAt()
    );
  }

  private void sendNotificationToMessagePartner(
      ChatRoom chatRoom,
      Long senderId,
      SenderType senderType,
      String content
  ) {
    try {
      NotificationReceiverInfo receiverInfo = determineReceiverInfoAndSave(
          chatRoom, senderId, senderType, content);

      notificationAsyncSender.sendMessageNotification(
          chatRoom.getId(),
          senderId,
          senderType,
          content,
          receiverInfo,
          NotificationType.MESSAGE
      );
    } catch (Exception e) {
      log.error("비동기 알림 전송 예외 발생", e);
    }
  }

  private NotificationReceiverInfo determineReceiverInfoAndSave(
      ChatRoom chatRoom,
      Long senderId,
      SenderType senderType,
      String content
  ) {
    if (senderType == SenderType.USER) {
      Notification savedNotification = saveNotificationAsHost(
          chatRoom.getHost(), content, NotificationType.MESSAGE);
      return determineHostReceiverInfo(savedNotification.getId(), chatRoom, senderId);
    } else if (senderType == SenderType.HOST) {
      Notification notification = saveNotificationAsUser(
          chatRoom.getUser(), content, NotificationType.MESSAGE);
      return determineUserReceiverInfo(notification.getId(), chatRoom, senderId);
    } else {
      throw new MeongnyangerangException(ErrorCode.NOTIFICATION_NOT_AUTHORIZED);
    }
  }

  private Notification saveNotificationAsUser(
      User user,
      String content,
      NotificationType notificationType
  ) {
    return notificationRepository.save(Notification.builder()
        .user(user)
        .content(content)
        .type(notificationType)
        .isRead(false)
        .build()
    );
  }

  private Notification saveNotificationAsHost(
      Host host,
      String content,
      NotificationType notificationType
  ) {
    return notificationRepository.save(Notification.builder()
        .host(host)
        .content(content)
        .type(notificationType)
        .isRead(false)
        .build()
    );
  }

  private NotificationReceiverInfo determineHostReceiverInfo(
      Long notificationId,
      ChatRoom chatRoom,
      Long userId
  ) {
    Long hostId = chatRoom.getHostId();
    User sender = findUserById(userId);
    Host receiver = findHostById(hostId);

    return new NotificationReceiverInfo(
        notificationId,
        hostId,
        SenderType.HOST,
        sender,
        receiver
    );
  }

  private NotificationReceiverInfo determineUserReceiverInfo(
      Long notificationId,
      ChatRoom chatRoom,
      Long hostId
  ) {
    Long userId = chatRoom.getUserId();
    Host sender = findHostById(hostId);
    User receiver = findUserById(userId);

    return new NotificationReceiverInfo(
        notificationId,
        userId,
        SenderType.USER,
        receiver,
        sender
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

  private ChatRoom findAndValidateChatRoom(Long chatRoomId, Long senderId, SenderType senderType) {
    ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.NOT_EXIST_CHAT_ROOM));
    validateChatRoomAccess(senderId, chatRoom, senderType); // 발신자가 채팅방 참여자인지 확인

    return chatRoom;
  }

  private void validateChatRoomAccess(Long viewerId, ChatRoom chatRoom, SenderType senderType) {
    if (senderType == SenderType.USER && !chatRoom.getUser().getId().equals(viewerId)) {
      throw new MeongnyangerangException(ErrorCode.CHAT_ROOM_NOT_AUTHORIZED);
    } else if (senderType == SenderType.HOST && !chatRoom.getHost().getId().equals(viewerId)) {
      throw new MeongnyangerangException(ErrorCode.CHAT_ROOM_NOT_AUTHORIZED);
    }
  }
}
