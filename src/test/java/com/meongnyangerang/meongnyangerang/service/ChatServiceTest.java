package com.meongnyangerang.meongnyangerang.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.chat.ChatMessage;
import com.meongnyangerang.meongnyangerang.domain.chat.ChatReadStatus;
import com.meongnyangerang.meongnyangerang.domain.chat.ChatRoom;
import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatRoomResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatMessageRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatReadStatusRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatRoomRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

  @Mock
  private ChatRoomRepository chatRoomRepository;

  @Mock
  private ChatMessageRepository chatMessageRepository;

  @Mock
  private ChatReadStatusRepository chatReadStatusRepository;

  @InjectMocks
  private ChatService chatService;

  private User user;
  private Host host;
  private ChatRoom chatRoom1;
  private ChatRoom chatRoom2;
  private ChatMessage lastMessage1;
  private ChatMessage lastMessage2;
  private ChatReadStatus userReadStatus;
  private ChatReadStatus hostReadStatus;
  private LocalDateTime now;

  private static final LocalDateTime DEFAULT_LAST_READ_TIME =
      LocalDateTime.of(2000, 1, 1, 0, 0);

  @BeforeEach
  void setUp() {
    now = LocalDateTime.now();

    // 사용자와 호스트 설정
    user = User.builder()
        .id(1L)
        .nickname("사용자1")
        .build();

    host = Host.builder()
        .id(2L)
        .nickname("호스트1")
        .build();

    // 채팅방 설정
    chatRoom1 = ChatRoom.builder()
        .id(1L)
        .user(user)
        .host(host)
        .createdAt(now.minusDays(1))
        .updatedAt(now)
        .build();

    chatRoom2 = ChatRoom.builder()
        .id(2L)
        .user(user)
        .host(host)
        .createdAt(now.minusDays(2))
        .updatedAt(now.minusHours(5))
        .build();

    // 마지막 메시지 설정
    lastMessage1 = ChatMessage.builder()
        .id(10L)
        .chatRoom(chatRoom1)
        .senderType(SenderType.HOST)
        .content("안녕하세요.")
        .createdAt(now)
        .build();

    lastMessage2 = ChatMessage.builder()
        .id(20L)
        .chatRoom(chatRoom2)
        .senderType(SenderType.USER)
        .content("문의 드립니다.")
        .createdAt(now.minusHours(5))
        .build();

    // 읽은 상태 설정
    userReadStatus = ChatReadStatus.builder()
        .id(1L)
        .chatRoomId(chatRoom1.getId())
        .participantId(user.getId())
        .participantType(SenderType.USER)
        .lastReadTime(now.minusHours(1))
        .build();

    hostReadStatus = ChatReadStatus.builder()
        .id(2L)
        .chatRoomId(chatRoom1.getId())
        .participantId(host.getId())
        .participantType(SenderType.HOST)
        .lastReadTime(now.minusMinutes(30))
        .build();
  }

  @Test
  @DisplayName("일반회원 관점에서 채팅방 목록 조회 성공")
  void getChatRoomsAsUser_Success() {
    // given
    List<ChatRoom> chatRooms = Arrays.asList(chatRoom1, chatRoom2);
    Long chatRoomId1 = chatRooms.get(0).getId();
    Long chatRoomId2 = chatRooms.get(1).getId();
    Long userId = user.getId();

    when(chatRoomRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(chatRooms);

    when(chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId1))
        .thenReturn(lastMessage1);
    when(chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId2))
        .thenReturn(lastMessage2);

    when(chatReadStatusRepository.findByChatRoomIdAndParticipantIdAndParticipantType(
        chatRoomId1, userId, SenderType.USER)).thenReturn(Optional.of(userReadStatus));

    when(chatReadStatusRepository.findByChatRoomIdAndParticipantIdAndParticipantType(
        chatRoomId2, userId, SenderType.USER)).thenReturn(Optional.empty());

    when(chatMessageRepository.countUnreadMessages(
        chatRoomId1, SenderType.HOST, userReadStatus.getLastReadTime())).thenReturn(2);
    when(chatMessageRepository.countUnreadMessages(
        chatRoomId2, SenderType.HOST, DEFAULT_LAST_READ_TIME)).thenReturn(0);

    // when
    List<ChatRoomResponse> responses = chatService.getChatRooms(userId, Role.ROLE_USER);

    // then
    assertThat(responses).hasSize(2);

    // 첫 번째 채팅방 검증
    ChatRoomResponse response1 = responses.get(0);
    assertThat(response1.chatRoomId()).isEqualTo(chatRoomId1);
    assertThat(response1.lastMessage()).isEqualTo("안녕하세요.");
    assertThat(response1.unreadCount()).isEqualTo(2);

    // 두 번째 채팅방 검증
    ChatRoomResponse response2 = responses.get(1);
    assertThat(response2.chatRoomId()).isEqualTo(chatRoomId2);
    assertThat(response2.lastMessage()).isEqualTo("문의 드립니다.");
    assertThat(response2.unreadCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("호스트 관점에서 채팅방 목록 조회 성공")
  void getChatRoomsAsHost_Success() {
    // given
    List<ChatRoom> chatRooms = Arrays.asList(chatRoom1, chatRoom2);
    Long chatRoomId1 = chatRooms.get(0).getId();
    Long chatRoomId2 = chatRooms.get(1).getId();
    Long hostId = host.getId();

    when(chatRoomRepository.findAllByHostIdOrderByUpdatedAtDesc(hostId)).thenReturn(chatRooms);

    when(chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId1))
        .thenReturn(lastMessage1);
    when(chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId2))
        .thenReturn(lastMessage2);

    when(chatReadStatusRepository.findByChatRoomIdAndParticipantIdAndParticipantType(
        chatRoomId1, hostId, SenderType.HOST)).thenReturn(Optional.of(userReadStatus));

    when(chatReadStatusRepository.findByChatRoomIdAndParticipantIdAndParticipantType(
        chatRoomId2, hostId, SenderType.HOST)).thenReturn(Optional.empty());

    when(chatMessageRepository.countUnreadMessages(
        chatRoomId1, SenderType.USER, userReadStatus.getLastReadTime())).thenReturn(0);
    when(chatMessageRepository.countUnreadMessages(
        chatRoomId2, SenderType.USER, DEFAULT_LAST_READ_TIME)).thenReturn(3);

    // when
    List<ChatRoomResponse> responses = chatService.getChatRooms(hostId, Role.ROLE_HOST);

    // then
    assertThat(responses).hasSize(2);

    // 첫 번째 채팅방 검증
    ChatRoomResponse response1 = responses.get(0);
    assertThat(response1.chatRoomId()).isEqualTo(chatRoomId1);
    assertThat(response1.lastMessage()).isEqualTo("안녕하세요.");
    assertThat(response1.unreadCount()).isEqualTo(0);

    // 두 번째 채팅방 검증
    ChatRoomResponse response2 = responses.get(1);
    assertThat(response2.chatRoomId()).isEqualTo(chatRoomId2);
    assertThat(response2.lastMessage()).isEqualTo("문의 드립니다.");
    assertThat(response2.unreadCount()).isEqualTo(3);
  }

  @Test
  @DisplayName("채팅방 목록 조회 실패 - 유효하지 않은 Role")
  void getChatRooms_InvalidRole_ThrowsException() {
    // given
    // when
    // then
    assertThatThrownBy(() -> chatService.getChatRooms(user.getId(), Role.ROLE_ADMIN))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.INVALID_AUTHORIZED);
  }
}