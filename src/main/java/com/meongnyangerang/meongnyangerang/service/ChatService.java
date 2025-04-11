package com.meongnyangerang.meongnyangerang.service;

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
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatReadStatusRepository chatReadStatusRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final UserRepository userRepository;
  private final AccommodationRepository accommodationRepository;
  private final SimpMessagingTemplate messagingTemplate;

  private static final String CHAT_DESTINATION = "/subscribe/chats/";
  private static final LocalDateTime DEFAULT_LAST_READ_TIME =
      LocalDateTime.of(2000, 1, 1, 0, 0);

  /**
   * 채팅방 생성
   */
  @Transactional
  public void createChatRoom(Long userId, Long accommodationId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.USER_NOT_FOUND));

    Accommodation accommodation = accommodationRepository.findById(accommodationId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.ACCOMMODATION_NOT_FOUND));

    Host host = accommodation.getHost();

    if (chatRoomRepository.existsByUser_IdAndHost_Id(user.getId(), host.getId())) {
      throw new MeongnyangerangException(ErrorCode.CHAT_ALREADY_EXISTS);
    }

    ChatRoom newChatRoom = createChatRoom(user, host);
    ChatRoom savedChatRoom = chatRoomRepository.save(newChatRoom);

    createReadStatusForParticipants(savedChatRoom, userId, host.getId()); // 읽음 상태 초기화
  }

  /**
   * 일반회원이 채팅방 목록 조회
   */
  public PageResponse<ChatRoomResponse> getChatRoomsAsUser(Long userId, Pageable pageable) {
    Page<ChatRoom> chatRooms = chatRoomRepository.findAllByUser_IdOrderByUpdatedAtDesc(
        userId, pageable);
    Page<ChatRoomResponse> response = chatRooms.map(this::createChatRoomResponseAsUser);

    return PageResponse.from(response);
  }

  /**
   * 호스트가 채팅방 목록 조회
   */
  public PageResponse<ChatRoomResponse> getChatRoomsAsHost(Long hostId, Pageable pageable) {
    Page<ChatRoom> chatRooms = chatRoomRepository.findAllByHost_IdOrderByUpdatedAtDesc(
        hostId, pageable);

    Page<ChatRoomResponse> response = chatRooms.map(this::createChatRoomResponseAsHost);

    return PageResponse.from(response);
  }

  /**
   * 메시지 이력 조회
   */
  @Transactional
  public ChatMessagesResponse getChatMessages(
      Long viewerId, Long chatRoomId, Long cursorId, int size, SenderType senderType
  ) {
    ChatRoom chatRoom = findAndValidateChatRoom(chatRoomId, viewerId, senderType);

    // 읽음 상태 업데이트 (채팅방에 들어왔으므로 메시지를 읽음으로 표시)
    updateReadStatus(chatRoom, viewerId, senderType);

    return getFetchMessageWithPaging(chatRoomId, cursorId, size);
  }

  /**
   * 메시지 전송 및 저장
   */
  @Transactional
  public void sendMessage(
      Long chatRoomId,
      String content,
      Long senderId,
      SenderType senderType
  ) {
    ChatRoom chatRoom = findAndValidateChatRoom(chatRoomId, senderId, senderType);
    ChatMessage message = createChatMessage(content, senderType, chatRoom);

    ChatMessage savedMessage = chatMessageRepository.save(message);
    chatRoom.updateLastActivity(); // 채팅방 업데이트 시간 갱신
    updateReadStatus(chatRoom, senderId, senderType);

    sendWebSocketMessage(chatRoomId, savedMessage);
  }

  private void updateReadStatus(ChatRoom chatRoom, Long participantId, SenderType senderType) {
    ChatReadStatus chatReadStatus = chatReadStatusRepository
        .findByChatRoomIdAndParticipantIdAndParticipantType(
            chatRoom.getId(), participantId, senderType)
        .orElseGet(() ->
            chatReadStatusRepository.save(ChatReadStatus.builder()
                .chatRoom(chatRoom)
                .participantId(participantId)
                .participantType(senderType)
                .build())
        );
    chatReadStatus.updateLastReadTime(LocalDateTime.now());
  }

  private ChatMessagesResponse getFetchMessageWithPaging(
      Long chatRoomId, Long cursorId, int size
  ) {
    Pageable pageable = PageRequest.of(0, size + 1);

    List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdWithCursor(
        chatRoomId, cursorId, pageable);

    boolean hasNext = messages.size() > size;

    List<ChatMessage> resultMessages = hasNext ? messages.subList(0, size) : messages;
    Long nextCursorId = hasNext ? messages.get(size - 1).getId() : null;

    List<ChatMessageResponse> messageResponses = resultMessages.stream()
        .map(ChatMessageResponse::from)
        .toList();

    return new ChatMessagesResponse(messageResponses, nextCursorId, hasNext);
  }

  private void sendWebSocketMessage(Long chatRoomId, ChatMessage savedMessage) {
    ChatMessageResponse payload = ChatMessageResponse.from(savedMessage);
    log.debug("메시지 전송: {}", CHAT_DESTINATION + chatRoomId);
    messagingTemplate.convertAndSend(CHAT_DESTINATION + chatRoomId, payload);
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

  private void createReadStatusForParticipants(ChatRoom chatRoom, Long userId, Long hostId) {
    ChatReadStatus userReadStatus = createChatReadStatus(chatRoom, userId, SenderType.USER);
    ChatReadStatus hostReadStatus = createChatReadStatus(chatRoom, hostId, SenderType.HOST);
    chatReadStatusRepository.save(userReadStatus);
    chatReadStatusRepository.save(hostReadStatus);
  }

  private static ChatReadStatus createChatReadStatus(
      ChatRoom chatRoom, Long participantId, SenderType participantType
  ) {
    return ChatReadStatus.builder()
        .chatRoom(chatRoom)
        .participantId(participantId)
        .participantType(participantType)
        .lastReadTime(LocalDateTime.now())
        .build();
  }

  private static ChatRoom createChatRoom(User user, Host host) {
    return ChatRoom.builder()
        .user(user)
        .host(host)
        .build();
  }

  private ChatRoomResponse createChatRoomResponseAsUser(ChatRoom chatRoom) {
    Accommodation accommodation = accommodationRepository.findByHostId(chatRoom.getHostId())
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.ACCOMMODATION_NOT_FOUND));

    ChatMessage lastMessage = chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(
        chatRoom.getId()); // 마지막 메시지 정보

    int unreadCount = chatMessageRepository.countUnreadMessages(
        chatRoom.getId(),
        SenderType.HOST, // 호스트가 보낸 메시지 중에서
        getLastReadTime(chatRoom.getId(), chatRoom.getUserId(), SenderType.USER)
    ); // 읽지 않은 메시지 수

    return new ChatRoomResponse(
        chatRoom.getId(),
        chatRoom.getHostId(),
        accommodation.getName(),
        accommodation.getThumbnailUrl(),
        getLastMessage(lastMessage),
        getLastMessageTime(lastMessage),
        unreadCount
    );
  }

  private ChatRoomResponse createChatRoomResponseAsHost(ChatRoom chatRoom) {
    ChatMessage lastMessage = chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(
        chatRoom.getId()); // 마지막 메시지 정보

    User user = chatRoom.getUser();
    int unreadCount = chatMessageRepository.countUnreadMessages(
        chatRoom.getId(),
        SenderType.USER, // 사용자가 보낸 메시지 중에서
        getLastReadTime(chatRoom.getId(), chatRoom.getHostId(), SenderType.HOST)
    ); // 읽지 않은 메시지 수

    return new ChatRoomResponse(
        chatRoom.getId(),
        user.getId(),
        user.getNickname(),
        user.getProfileImage(),
        getLastMessage(lastMessage),
        getLastMessageTime(lastMessage),
        unreadCount
    );
  }

  private LocalDateTime getLastReadTime(
      Long chatRoomId,
      Long viewerId,
      SenderType viewerType
  ) {
    return chatReadStatusRepository
        .findByChatRoomIdAndParticipantIdAndParticipantType(chatRoomId, viewerId, viewerType)
        .map(ChatReadStatus::getLastReadTime)
        .orElse(DEFAULT_LAST_READ_TIME);
  }

  private static String getLastMessage(ChatMessage lastMessage) {
    return lastMessage != null ? lastMessage.getContent() : "";
  }

  private static LocalDateTime getLastMessageTime(ChatMessage lastMessage) {
    return lastMessage != null ? lastMessage.getCreatedAt() : null;
  }

  private static ChatMessage createChatMessage(
      String content, SenderType senderType, ChatRoom chatRoom
  ) {
    return ChatMessage.builder()
        .chatRoom(chatRoom)
        .senderType(senderType)
        .content(content)
        .build();
  }
}
