package com.meongnyangerang.meongnyangerang.service.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.chat.ChatRoom;
import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.notification.Notification;
import com.meongnyangerang.meongnyangerang.domain.notification.NotificationType;
import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.dto.notification.MessageNotificationRequest;
import com.meongnyangerang.meongnyangerang.dto.notification.NotificationResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.NotificationRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatRoomRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private ChatRoomRepository chatRoomRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private HostRepository hostRepository;

  @Mock
  private NotificationAsyncService notificationAsyncSender;

  @InjectMocks
  private NotificationService notificationService;

  private User user;
  private Host host;
  private ChatRoom chatRoom;
  private static final String CONTENT = "테스트 메시지 내용";

  @BeforeEach
  void setUp() {
    user = new User();
    user.setId(1L);

    host = new Host();
    host.setId(2L);

    chatRoom = ChatRoom.builder()
        .id(3L)
        .user(user)
        .host(host)
        .build();
  }

  @Test
  @DisplayName("알림 전송 성공 - 사용자가 발신자인 경우 ")
  void sendNotification_WhenUserIsSender_Success() {
    // given
    MessageNotificationRequest request = new MessageNotificationRequest(
        chatRoom.getId(),
        CONTENT
    );
    when(chatRoomRepository.findById(chatRoom.getId())).thenReturn(Optional.of(chatRoom));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(hostRepository.findById(host.getId())).thenReturn(Optional.of(host));
    ArgumentCaptor<Notification> notificationArgumentCaptor =
        ArgumentCaptor.forClass(Notification.class);

    // when
    notificationService.sendMessageNotification(request, user.getId(), SenderType.USER);

    // Then
    verify(notificationRepository).save(notificationArgumentCaptor.capture());
    Notification notification = notificationArgumentCaptor.getValue();
    assertEquals(host, notification.getHost());
    assertEquals(CONTENT, notification.getContent());
    assertEquals(NotificationType.MESSAGE, notification.getType());
    assertFalse(notification.getIsRead());

    verify(notificationAsyncSender).sendMessageNotification(
        chatRoom.getId(),
        user.getId(),
        SenderType.USER,
        CONTENT,
        host.getId(),
        SenderType.HOST,
        NotificationType.MESSAGE
    );
  }

  @Test
  @DisplayName("알림 전송 성공 - 호스트가 발신자인 경우 ")
  void sendNotification_WhenHostIsSender_Success() {
    // given
    MessageNotificationRequest request = new MessageNotificationRequest(
        chatRoom.getId(),
        CONTENT
    );
    when(chatRoomRepository.findById(chatRoom.getId())).thenReturn(Optional.of(chatRoom));
    when(hostRepository.findById(host.getId())).thenReturn(Optional.of(host));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    ArgumentCaptor<Notification> notificationArgumentCaptor =
        ArgumentCaptor.forClass(Notification.class);

    // when
    notificationService.sendMessageNotification(request, host.getId(), SenderType.HOST);

    // Then
    verify(notificationRepository).save(notificationArgumentCaptor.capture());
    Notification notification = notificationArgumentCaptor.getValue();
    assertEquals(user, notification.getUser());
    assertEquals(CONTENT, notification.getContent());
    assertEquals(NotificationType.MESSAGE, notification.getType());
    assertFalse(notification.getIsRead());

    verify(notificationAsyncSender).sendMessageNotification(
        chatRoom.getId(),
        host.getId(),
        SenderType.HOST,
        CONTENT,
        user.getId(),
        SenderType.USER,
        NotificationType.MESSAGE
    );
  }

  @Test
  @DisplayName("알림 전송 실패 - 존재하지 않는 채팅방")
  void sendNotification_NotExistsChatRoom_ThrowsException() {
    // given
    MessageNotificationRequest request = new MessageNotificationRequest(
        chatRoom.getId(),
        CONTENT
    );
    when(chatRoomRepository.findById(chatRoom.getId())).thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(
        () -> notificationService.sendMessageNotification(request, user.getId(), SenderType.USER))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.NOT_EXIST_CHAT_ROOM);
  }

  @Test
  @DisplayName("알림 전송 실패 - 권한 없는 사용자")
  void sendNotification_InvalidAuthorization_ThrowsException() {
    // given
    MessageNotificationRequest request = new MessageNotificationRequest(
        chatRoom.getId(),
        CONTENT
    );
    when(chatRoomRepository.findById(chatRoom.getId())).thenReturn(Optional.of(chatRoom));

    // when
    // then
    assertThatThrownBy(
        () -> notificationService.sendMessageNotification(request, 999L, SenderType.HOST))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.CHAT_ROOM_NOT_AUTHORIZED);
  }

  @Test
  @DisplayName("알림 목록 조회 성공 - 사용자")
  void getNotificationsAsUser_Success() {
    // given
    final int SIZE = 20;
    final String CONTENT = "호스트에게 알림";
    Notification notification1 = createNotificationReceiveByUser(
        1L, user, CONTENT + 1);
    Notification notification2 = createNotificationReceiveByUser(
        2L, user, CONTENT + 2);
    List<Notification> notifications = Arrays.asList(notification1, notification2);

    Pageable pageable = PageRequest.of(0, SIZE);
    Page<Notification> notificationPage = new PageImpl<>(
        notifications, pageable, notifications.size());

    when(notificationRepository.findAllByUser_IdOrderByCreatedAtDesc(user.getId(), pageable))
        .thenReturn(notificationPage);

    // when
    PageResponse<NotificationResponse> response = notificationService.getNotificationsAsUser(
        user.getId(), pageable);

    // then
    assertTrue(notification1.getIsRead());
    assertTrue(notification2.getIsRead());
    assertEquals(0, response.page());
    assertEquals(SIZE, response.size());
    assertEquals(2, response.totalElements());
    assertEquals(0, response.page());
    assertTrue(response.first());
    assertTrue(response.last());

    List<NotificationResponse> contents = response.content();
    assertEquals(2, contents.size());

    NotificationResponse content1 = contents.get(0);
    assertEquals(1L, content1.notificationId());
    assertEquals(CONTENT + 1, content1.content());
    assertEquals(NotificationType.MESSAGE, content1.notificationType());

    NotificationResponse content2 = contents.get(1);
    assertEquals(2L, content2.notificationId());
    assertEquals(CONTENT + 2, content2.content());
    assertEquals(NotificationType.MESSAGE, content2.notificationType());

    verify(notificationRepository).findAllByUser_IdOrderByCreatedAtDesc(user.getId(), pageable);
  }

  @Test
  @DisplayName("알림 목록 조회 성공 - 호스트")
  void getNotificationsAsHost_Success() {
    // given
    final int SIZE = 20;
    final String CONTENT = "사용자에게 알림";
    Notification notification1 = createNotificationReceiveByHost(
        1L, host, CONTENT + 1);
    Notification notification2 = createNotificationReceiveByHost(
        2L, host, CONTENT + 2);
    List<Notification> notifications = Arrays.asList(notification1, notification2);

    Pageable pageable = PageRequest.of(0, SIZE);
    Page<Notification> notificationPage = new PageImpl<>(
        notifications, pageable, notifications.size());

    when(notificationRepository.findAllByHost_IdOrderByCreatedAtDesc(host.getId(), pageable))
        .thenReturn(notificationPage);

    // when
    PageResponse<NotificationResponse> response = notificationService.getNotificationsAsHost(
        host.getId(), pageable);

    // then
    assertTrue(notification1.getIsRead());
    assertTrue(notification2.getIsRead());
    assertEquals(0, response.page());
    assertEquals(SIZE, response.size());
    assertEquals(2, response.totalElements());
    assertEquals(0, response.page());
    assertTrue(response.first());
    assertTrue(response.last());

    List<NotificationResponse> contents = response.content();
    assertEquals(2, contents.size());

    NotificationResponse content1 = contents.get(0);
    assertEquals(1L, content1.notificationId());
    assertEquals(CONTENT + 1, content1.content());
    assertEquals(NotificationType.MESSAGE, content1.notificationType());

    NotificationResponse content2 = contents.get(1);
    assertEquals(2L, content2.notificationId());
    assertEquals(CONTENT + 2, content2.content());
    assertEquals(NotificationType.MESSAGE, content2.notificationType());

    verify(notificationRepository).findAllByHost_IdOrderByCreatedAtDesc(host.getId(), pageable);
  }

  @Test
  @DisplayName("알림 삭제 - 사용자")
  void deleteNotification_AsUser_Success() {
    // given
    Long userId = user.getId();
    Long notificationId = 1L;
    Notification notification = createNotificationReceiveByUser(
        notificationId, user, "사용자 알림");

    // when
    notificationService.deleteNotificationAsUser(notification.getId(), userId);

    // then
    verify(notificationRepository).deleteByIdAndUser_Id(notificationId, userId);
  }

  @Test
  @DisplayName("알림 삭제 - 호스트")
  void deleteNotification_AsHost_Success() {
    // given
    Long hostId = host.getId();
    Long notificationId = 1L;
    Notification notification = createNotificationReceiveByHost(
        notificationId, host, "호스트 알림");

    // when
    notificationService.deleteNotificationAsHost(notification.getId(), hostId);

    // then
    verify(notificationRepository).deleteByIdAndHost_Id(notificationId, hostId);
  }

  @Test
  @DisplayName("예약 리마인드 알림 발송 성공")
  void sendReservationReminderNotification_Success() {
    // given
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    Reservation reservation = Reservation.builder()
        .id(1L)
        .user(user)
        .accommodationName("테스트 숙소 이름")
        .checkInDate(tomorrow)
        .checkInDate(tomorrow.plusDays(1))
        .status(ReservationStatus.RESERVED)
        .build();
    String content = String.format("%s 숙소 체크인이 내일입니다. 체크인 시간은 %s입니다.",
        reservation.getAccommodationName(), reservation.getCheckInDate());

    // when
    notificationService.sendReservationReminderNotification(reservation);

    // then
    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(
        Notification.class);
    verify(notificationRepository, times(1))
        .save(notificationArgumentCaptor.capture());
    verify(notificationAsyncSender, times(1))
        .sendReservationNotification(
            reservation.getId(),
            content,
            user.getId(),
            SenderType.USER,
            NotificationType.RESERVATION_REMINDER);
  }

  private Notification createNotificationReceiveByHost(Long id, Host host, String content) {
    return Notification.builder()
        .id(id)
        .host(host)
        .content(content)
        .type(NotificationType.MESSAGE)
        .isRead(false)
        .createdAt(LocalDateTime.now())
        .build();
  }

  private Notification createNotificationReceiveByUser(Long id, User user, String content) {
    return Notification.builder()
        .id(id)
        .user(user)
        .content(content)
        .type(NotificationType.MESSAGE)
        .isRead(false)
        .createdAt(LocalDateTime.now())
        .build();
  }
}