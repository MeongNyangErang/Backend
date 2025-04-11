package com.meongnyangerang.meongnyangerang.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.chat.ChatMessage;
import com.meongnyangerang.meongnyangerang.domain.chat.ChatReadStatus;
import com.meongnyangerang.meongnyangerang.domain.chat.ChatRoom;
import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatMessageResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatMessagesResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatRoomResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatMessageRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatReadStatusRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatRoomRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

  @Mock
  private ChatRoomRepository chatRoomRepository;

  @Mock
  private ChatMessageRepository chatMessageRepository;

  @Mock
  private ChatReadStatusRepository chatReadStatusRepository;

  @Mock
  private AccommodationRepository accommodationRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private ChatService chatService;

  private User user;
  private Host host;
  private Accommodation accommodation;
  private ChatRoom chatRoom1;
  private ChatRoom chatRoom2;
  private ChatMessage lastMessage1;
  private ChatMessage lastMessage2;
  private ChatReadStatus userReadStatus;
  private ChatMessage chatMessage;
  private final LocalDateTime now = LocalDateTime.now();

  private static final String CHAT_DESTINATION = "/subscribe/chat";
  private static final LocalDateTime DEFAULT_LAST_READ_TIME =
      LocalDateTime.of(2000, 1, 1, 0, 0);

  @BeforeEach
  void setUp() {
    // 사용자와 호스트 설정
    user = User.builder()
        .id(1L)
        .nickname("사용자1")
        .build();

    host = Host.builder()
        .id(2L)
        .nickname("호스트1")
        .build();

    accommodation = Accommodation.builder()
        .id(3L)
        .host(host)
        .name("숙소")
        .thumbnailUrl("test-accommodation-thumbnail.jpg")
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
        .chatRoom(chatRoom1)
        .participantId(user.getId())
        .participantType(SenderType.USER)
        .lastReadTime(now.minusHours(1))
        .build();

    chatMessage = ChatMessage.builder()
        .id(1L)
        .chatRoom(chatRoom1)
        .content("테스트 메시지")
        .senderType(SenderType.USER)
        .build();
  }

  @Test
  @DisplayName("채팅방 생성 성공")
  public void createChatRoom_Success() {
    // given
    Long userId = user.getId();
    Long hostId = accommodation.getHost().getId();
    Long accommodationId = accommodation.getId();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(accommodationRepository.findById(accommodationId)).thenReturn(Optional.of(accommodation));
    when(chatRoomRepository.existsByUser_IdAndHost_Id(userId, hostId)).thenReturn(false);

    ArgumentCaptor<ChatRoom> chatRoomArgumentCaptor = ArgumentCaptor.forClass(ChatRoom.class);
    when(chatRoomRepository.save(chatRoomArgumentCaptor.capture())).thenReturn(chatRoom1);

    // when
    chatService.createChatRoom(userId, accommodationId);

    // then
    verify(userRepository, times(1)).findById(userId);
    verify(accommodationRepository, times(1))
        .findById(accommodationId);
    verify(chatRoomRepository, times(1))
        .existsByUser_IdAndHost_Id(userId, hostId);
    verify(chatRoomRepository, times(1))
        .save(chatRoomArgumentCaptor.capture());
  }

  @Test
  @DisplayName("채팅방 생성 실패 - 존재하지 않는 일반회원")
  void createChatRoom_UserNotFound_ThrowsException() {
    // given
    Long userId = user.getId();
    Long accommodationId = accommodation.getId();

    when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(() -> chatService.createChatRoom(userId, accommodationId))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.USER_NOT_FOUND);

    verify(userRepository, times(1)).findById(userId);
  }

  @Test
  @DisplayName("채팅방 생성 실패 - 존재하지 않는 숙소")
  void createChatRoom_AccommodationNotFound_ThrowsException() {
    // given
    Long userId = user.getId();
    Long accommodationId = accommodation.getId();

    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(accommodationRepository.findById(accommodationId)).thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(() -> chatService.createChatRoom(userId, accommodationId))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.ACCOMMODATION_NOT_FOUND);

    verify(userRepository, times(1)).findById(userId);
    verify(accommodationRepository, times(1))
        .findById(accommodationId);
  }

  @Test
  @DisplayName("채팅방 생성 실패 - 이미 채팅방 존재")
  void createChatRoom_AlreadyExists_ThrowsException() {
    // given
    Long userId = user.getId();
    Long hostId = accommodation.getHost().getId();
    Long accommodationId = accommodation.getId();

    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(accommodationRepository.findById(accommodationId)).thenReturn(Optional.of(accommodation));
    when(chatRoomRepository.existsByUser_IdAndHost_Id(userId, hostId)).thenReturn(true);

    // when
    // then
    assertThatThrownBy(() -> chatService.createChatRoom(userId, accommodationId))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.CHAT_ALREADY_EXISTS);

    verify(userRepository, times(1)).findById(userId);
    verify(accommodationRepository, times(1))
        .findById(accommodationId);
    verify(chatRoomRepository, times(1))
        .existsByUser_IdAndHost_Id(userId, hostId);
  }

  @Test
  @DisplayName("일반회원 관점에서 채팅방 목록 조회 성공")
  void getChatRoomsAsUser_Success() {
    // given
    List<ChatRoom> chatRooms = Arrays.asList(chatRoom1, chatRoom2);
    Long chatRoomId1 = chatRooms.get(0).getId();
    Long chatRoomId2 = chatRooms.get(1).getId();
    Long userId = user.getId();
    SenderType senderType = SenderType.USER;

    Pageable pageable = PageRequest.of(
        0, 10, Sort.by("updatedAt").descending());
    Page<ChatRoom> pagedChatRooms = new PageImpl<>(chatRooms, pageable, chatRooms.size());

    when(chatRoomRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId, pageable))
        .thenReturn(pagedChatRooms);

    when(accommodationRepository.findByHostId(chatRooms.get(0).getHostId()))
        .thenReturn(Optional.of(accommodation));

    when(accommodationRepository.findByHostId(chatRooms.get(1).getHostId()))
        .thenReturn(Optional.of(accommodation));

    when(chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId1))
        .thenReturn(lastMessage1);
    when(chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId2))
        .thenReturn(lastMessage2);

    when(chatReadStatusRepository.findByChatRoomIdAndParticipantIdAndParticipantType(
        chatRoomId1, userId, senderType)).thenReturn(Optional.of(userReadStatus));

    when(chatReadStatusRepository.findByChatRoomIdAndParticipantIdAndParticipantType(
        chatRoomId2, userId, senderType)).thenReturn(Optional.empty());

    when(chatMessageRepository.countUnreadMessages(
        chatRoomId1, SenderType.HOST, userReadStatus.getLastReadTime())).thenReturn(2);
    when(chatMessageRepository.countUnreadMessages(
        chatRoomId2, SenderType.HOST, DEFAULT_LAST_READ_TIME)).thenReturn(0);

    // when
    PageResponse<ChatRoomResponse> responses = chatService.getChatRoomsAsUser(userId, pageable);

    // then
    assertEquals(2, responses.content().size());

    // 첫 번째 채팅방 검증
    ChatRoomResponse response1 = responses.content().get(0);
    assertThat(response1.chatRoomId()).isEqualTo(chatRoomId1);
    assertThat(response1.partnerId()).isEqualTo(chatRooms.get(0).getHostId());
    assertThat(response1.partnerName()).isEqualTo(accommodation.getName());
    assertThat(response1.partnerImageUrl()).isEqualTo(accommodation.getThumbnailUrl());
    assertThat(response1.lastMessage()).isEqualTo("안녕하세요.");
    assertThat(response1.unreadCount()).isEqualTo(2);

    // 두 번째 채팅방 검증
    ChatRoomResponse response2 = responses.content().get(1);
    assertThat(response2.chatRoomId()).isEqualTo(chatRoomId2);
    assertThat(response1.partnerId()).isEqualTo(chatRooms.get(1).getHostId());
    assertThat(response1.partnerName()).isEqualTo(accommodation.getName());
    assertThat(response1.partnerImageUrl()).isEqualTo(accommodation.getThumbnailUrl());
    assertThat(response2.lastMessage()).isEqualTo("문의 드립니다.");
    assertThat(response2.unreadCount()).isEqualTo(0);

    // 메서드 호출 검증
    verify(chatRoomRepository, times(1))
        .findAllByUser_IdOrderByUpdatedAtDesc(userId, pageable);
    verify(accommodationRepository, times(2))
        .findByHostId(chatRooms.get(0).getHostId());
    verify(chatMessageRepository, times(1))
        .findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId1);
    verify(chatMessageRepository, times(1))
        .findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId2);
    verify(chatReadStatusRepository, times(1))
        .findByChatRoomIdAndParticipantIdAndParticipantType(chatRoomId1, userId, senderType);
  }

  @Test
  @DisplayName("호스트 관점에서 채팅방 목록 조회 성공")
  void getChatRoomsAsHost_Success() {
    // given
    List<ChatRoom> chatRooms = Arrays.asList(chatRoom1, chatRoom2);
    Long chatRoomId1 = chatRooms.get(0).getId();
    Long chatRoomId2 = chatRooms.get(1).getId();
    Long hostId = host.getId();
    SenderType senderType = SenderType.HOST;

    Pageable pageable = PageRequest.of(
        0, 10, Sort.by("updatedAt").descending());
    Page<ChatRoom> pagedChatRooms = new PageImpl<>(chatRooms, pageable, chatRooms.size());

    when(chatRoomRepository.findAllByHost_IdOrderByUpdatedAtDesc(hostId, pageable))
        .thenReturn(pagedChatRooms);

    when(chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId1))
        .thenReturn(lastMessage1);
    when(chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId2))
        .thenReturn(lastMessage2);

    when(chatReadStatusRepository.findByChatRoomIdAndParticipantIdAndParticipantType(
        chatRoomId1, hostId, senderType)).thenReturn(Optional.of(userReadStatus));

    when(chatReadStatusRepository.findByChatRoomIdAndParticipantIdAndParticipantType(
        chatRoomId2, hostId, senderType)).thenReturn(Optional.empty());

    when(chatMessageRepository.countUnreadMessages(
        chatRoomId1, SenderType.USER, userReadStatus.getLastReadTime())).thenReturn(0);
    when(chatMessageRepository.countUnreadMessages(
        chatRoomId2, SenderType.USER, DEFAULT_LAST_READ_TIME)).thenReturn(3);

    // when
    PageResponse<ChatRoomResponse> responses = chatService.getChatRoomsAsHost(hostId, pageable);

    // then
    assertEquals(2, responses.content().size());

    // 첫 번째 채팅방 검증
    ChatRoomResponse response1 = responses.content().get(0);
    assertThat(response1.chatRoomId()).isEqualTo(chatRoomId1);
    assertThat(response1.lastMessage()).isEqualTo("안녕하세요.");
    assertThat(response1.unreadCount()).isEqualTo(0);

    // 두 번째 채팅방 검증
    ChatRoomResponse response2 = responses.content().get(1);
    assertThat(response2.chatRoomId()).isEqualTo(chatRoomId2);
    assertThat(response2.lastMessage()).isEqualTo("문의 드립니다.");
    assertThat(response2.unreadCount()).isEqualTo(3);

    // 메서드 호출 검증
    verify(chatRoomRepository).findAllByHost_IdOrderByUpdatedAtDesc(hostId, pageable);
    verify(chatMessageRepository, times(1))
        .findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId1);
    verify(chatMessageRepository, times(1))
        .findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId2);
    verify(chatReadStatusRepository, times(1))
        .findByChatRoomIdAndParticipantIdAndParticipantType(chatRoomId1, hostId, senderType);
  }

  @Test
  @DisplayName("존재하지 않는 채팅방 목록 조회")
  void getChatRooms_withEmptyResult_shouldReturnEmptyPage() {
    // given
    Long userId = user.getId();
    Pageable pageable = PageRequest.of(
        0, 10, Sort.by("updatedAt").descending());

    // 빈 페이지 반환
    Page<ChatRoom> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

    when(chatRoomRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId, pageable))
        .thenReturn(emptyPage);

    // when
    PageResponse<ChatRoomResponse> response = chatService.getChatRoomsAsUser(userId, pageable);

    // then
    assertNotNull(response);
  }

  @Test
  @DisplayName("메시지 조회 성공")
  void getChatMessages_Success() {
    // given
    Long userId = user.getId();
    Long chatRoomId = chatRoom1.getId();
    Long cursorId = null;
    int size = 5;
    ChatReadStatus chatReadStatus = ChatReadStatus.builder()
        .chatRoom(chatRoom1)
        .participantId(userId)
        .participantType(SenderType.USER)
        .build();

    Pageable pageable = PageRequest.of(0, size + 1);

    when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom1));
    when(chatMessageRepository.findByChatRoomIdWithCursor(chatRoomId, cursorId, pageable))
        .thenReturn(createTestMessageResponses(chatRoom1, size));
    when(chatReadStatusRepository.findByChatRoomIdAndParticipantIdAndParticipantType(
        chatRoomId, userId, SenderType.USER)).thenReturn(Optional.of(chatReadStatus));

    // when
    ChatMessagesResponse response = chatService.getChatMessages(
        userId, chatRoomId, cursorId, size, SenderType.USER);

    // then
    List<ChatMessageResponse> messages = response.messages();
    for (int i = 0; i < size; i++) {
      assertThat(messages.get(i).messageId()).isEqualTo(size - i);
      assertThat(messages.get(i).senderType())
          .isEqualTo(i % 2 == 0 ? SenderType.USER : SenderType.HOST);
      assertThat(messages.get(i).content()).isEqualTo("Test message " + i);
    }
    assertThat(response.nextCursorId()).isNull();
    assertThat(response.hasNext()).isFalse();

    verify(chatRoomRepository, times(1)).findById(chatRoomId);
    verify(chatMessageRepository, times(1))
        .findByChatRoomIdWithCursor(chatRoomId, cursorId, pageable);
    verify(chatReadStatusRepository, times(1))
        .findByChatRoomIdAndParticipantIdAndParticipantType(chatRoomId, userId, SenderType.USER);
  }

  @Test
  @DisplayName("메시지 조회 실패 - 존재하지 않는 채팅방")
  void getChatMessages_ChatRoomNotExists_ThrowsException() {
    // given
    Long userId = user.getId();
    Long chatRoomId = chatRoom1.getId();
    Long cursorId = null;
    int size = 5;

    // when
    // then
    assertThatThrownBy(
        () -> chatService.getChatMessages(userId, chatRoomId, cursorId, size, SenderType.USER))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.NOT_EXIST_CHAT_ROOM);

    verify(chatRoomRepository, times(1)).findById(chatRoomId);
  }

  @Test
  @DisplayName("메시지 조회 실패 - 채팅방 권한 없음")
  void getChatMessages_NotAuthorized_ThrowsException() {
    // given
    User notAuthorizedUser = User.builder()
        .id(10L)
        .build();
    Long chatRoomId = chatRoom1.getId();
    Long cursorId = null;
    int size = 5;

    when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom1));

    // when
    // then
    assertThatThrownBy(() -> chatService.getChatMessages(
        notAuthorizedUser.getId(), chatRoomId, cursorId, size, SenderType.USER))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.CHAT_ROOM_NOT_AUTHORIZED);

    verify(chatRoomRepository, times(1)).findById(chatRoomId);
  }

  @Test
  @DisplayName("메시지 전송 성공")
  void sendMessage_Success() {
    // given
    Long chatRoomId = chatRoom1.getId();
    String content = "안녕하세요";
    Long senderId = user.getId();
    SenderType senderType = SenderType.USER;
    ArgumentCaptor<ChatMessage> chatRoomArgumentCaptor = ArgumentCaptor.forClass(ChatMessage.class);
    ChatMessageResponse chatMessageResponse = createChatMessageResponse(chatMessage);

    when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom1));
    when(chatMessageRepository.save(chatRoomArgumentCaptor.capture())).thenReturn(chatMessage);
    when(chatReadStatusRepository.findByChatRoomIdAndParticipantIdAndParticipantType(
        chatRoomId, senderId, senderType)).thenReturn(Optional.of(userReadStatus));

    // when
    chatService.sendMessage(chatRoomId, content, senderId, senderType);

    // then
    verify(chatRoomRepository).findById(chatRoomId);
    verify(chatMessageRepository).save(chatRoomArgumentCaptor.capture());
    verify(chatReadStatusRepository).findByChatRoomIdAndParticipantIdAndParticipantType(
        chatRoomId, senderId, senderType);
    verify(messagingTemplate).convertAndSend(
        CHAT_DESTINATION + chatRoomId, chatMessageResponse);
    verify(notificationService).sendNotificationToMessagePartner(chatRoom1, senderId, senderType,
        content);
  }

  @Test
  @DisplayName("메시지 전송 성공 - 읽음 상태가 없으면 새로 생성")
  void sendMessage_NotExistsReadStatusWhenCreateReadStatus_Success() {
    // given
    Long chatRoomId = chatRoom1.getId();
    String content = "안녕하세요";
    Long senderId = user.getId();
    SenderType senderType = SenderType.USER;
    ArgumentCaptor<ChatMessage> chatRoomArgumentCaptor = ArgumentCaptor.forClass(ChatMessage.class);
    ArgumentCaptor<ChatReadStatus> chatReadStatusArgumentCaptor =
        ArgumentCaptor.forClass(ChatReadStatus.class);
    ChatMessageResponse chatMessageResponse = createChatMessageResponse(chatMessage);

    when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom1));
    when(chatMessageRepository.save(chatRoomArgumentCaptor.capture())).thenReturn(chatMessage);
    when(chatReadStatusRepository.findByChatRoomIdAndParticipantIdAndParticipantType(
        chatRoomId, senderId, senderType)).thenReturn(Optional.empty());
    when(chatReadStatusRepository.save(chatReadStatusArgumentCaptor.capture()))
        .thenReturn(userReadStatus);

    // when
    chatService.sendMessage(chatRoomId, content, senderId, senderType);

    // then
    verify(chatRoomRepository).findById(chatRoomId);
    verify(chatMessageRepository).save(chatRoomArgumentCaptor.capture());
    verify(chatReadStatusRepository).findByChatRoomIdAndParticipantIdAndParticipantType(
        chatRoomId, senderId, senderType);
    verify(chatReadStatusRepository).save(chatReadStatusArgumentCaptor.capture());
    verify(messagingTemplate).convertAndSend(
        CHAT_DESTINATION + chatRoomId, chatMessageResponse);
    verify(notificationService).sendNotificationToMessagePartner(
        chatRoom1, senderId, senderType, content);
  }

  @Test
  @DisplayName("메시지 전송 실패 - 존재하지 않는 채팅방")
  void sendMessage_NotExistsChatRoom_ThrowsException() {
    // given
    Long chatRoomId = 999L;
    String content = "안녕하세요";
    Long senderId = user.getId();
    SenderType senderType = SenderType.USER;
    ChatMessageResponse chatMessageResponse = createChatMessageResponse(chatMessage);

    when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(() -> chatService.sendMessage(chatRoomId, content, senderId, senderType))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.NOT_EXIST_CHAT_ROOM);

    verify(chatRoomRepository).findById(chatRoomId);
    verify(chatMessageRepository, never()).save(chatMessage);
    verify(messagingTemplate, never()).convertAndSend(chatMessageResponse);
    verify(notificationService, never()).sendNotificationToMessagePartner(
        chatRoom1, senderId, senderType, content);
  }

  @Test
  @DisplayName("메시지 전송 실패 - 권한 없음")
  void sendMessage_NotUnauthorized_ThrowsException() {
    // given
    Long chatRoomId = chatRoom1.getId();
    String content = "안녕하세요";
    Long senderId = 3L; // 채팅방에 속하지 않은 사용자
    SenderType senderType = SenderType.USER;
    ChatMessageResponse chatMessageResponse = createChatMessageResponse(chatMessage);

    when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom1));

    // when
    // then
    assertThatThrownBy(() -> chatService.sendMessage(chatRoomId, content, senderId, senderType))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.CHAT_ROOM_NOT_AUTHORIZED);

    verify(chatRoomRepository).findById(chatRoomId);
    verify(chatMessageRepository, never()).save(chatMessage);
    verify(messagingTemplate, never()).convertAndSend(chatMessageResponse);
    verify(notificationService, never()).sendNotificationToMessagePartner(
        chatRoom1, senderId, senderType, content);
  }

  private List<ChatMessage> createTestMessageResponses(ChatRoom chatRoom, int size) {
    List<ChatMessage> messages = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      messages.add(new ChatMessage(
          (long) (size - i),
          chatRoom,
          i % 2 == 0 ? SenderType.USER : SenderType.HOST,
          "Test message " + i,
          LocalDateTime.now().minusMinutes(i)
      ));
    }
    return messages;
  }

  private static ChatMessageResponse createChatMessageResponse(ChatMessage chatMessage) {
    return new ChatMessageResponse(
        chatMessage.getId(),
        chatMessage.getSenderType(),
        chatMessage.getContent(),
        chatMessage.getCreatedAt()
    );
  }
}