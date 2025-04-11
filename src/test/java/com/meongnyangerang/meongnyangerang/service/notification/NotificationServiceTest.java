package com.meongnyangerang.meongnyangerang.service.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.chat.ChatRoom;
import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.notification.Notification;
import com.meongnyangerang.meongnyangerang.domain.notification.NotificationType;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.notification.MessageNotificationRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.NotificationRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatRoomRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    notificationService.sendNotification(request, user.getId(), SenderType.USER);

    // Then
    verify(notificationRepository).save(notificationArgumentCaptor.capture());
    Notification notification = notificationArgumentCaptor.getValue();
    assertEquals(host, notification.getHost());
    assertEquals(CONTENT, notification.getContent());
    assertEquals(NotificationType.MESSAGE, notification.getType());
    assertFalse(notification.getIsRead());

    verify(notificationAsyncSender).sendWebSocketNotification(
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
    notificationService.sendNotification(request, host.getId(), SenderType.HOST);

    // Then
    verify(notificationRepository).save(notificationArgumentCaptor.capture());
    Notification notification = notificationArgumentCaptor.getValue();
    assertEquals(user, notification.getUser());
    assertEquals(CONTENT, notification.getContent());
    assertEquals(NotificationType.MESSAGE, notification.getType());
    assertFalse(notification.getIsRead());

    verify(notificationAsyncSender).sendWebSocketNotification(
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
        () -> notificationService.sendNotification(request, user.getId(), SenderType.USER))
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
        () -> notificationService.sendNotification(request, 999L, SenderType.HOST))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.CHAT_ROOM_NOT_AUTHORIZED);
  }
}