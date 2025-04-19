package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.chat.ChatMessage;
import com.meongnyangerang.meongnyangerang.domain.chat.ChatReadStatus;
import com.meongnyangerang.meongnyangerang.domain.chat.ChatRoom;
import com.meongnyangerang.meongnyangerang.domain.chat.MessageType;
import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatCreateResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatMessageResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatRoomResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatMessageRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatReadStatusRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatRoomRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
  private final ImageService imageService;

  private static final String CHAT_DESTINATION = "/subscribe/chats/";
  private static final LocalDateTime DEFAULT_LAST_READ_TIME =
      LocalDateTime.of(2000, 1, 1, 0, 0);

  /**
   * 채팅방 생성
   */
  @Transactional
  public ChatCreateResponse createChatRoom(Long userId, Long accommodationId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.USER_NOT_FOUND));

    Accommodation accommodation = accommodationRepository.findById(accommodationId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.ACCOMMODATION_NOT_FOUND));

    Host host = accommodation.getHost();

    ChatRoom chatRoom = chatRoomRepository.findByUser_IdAndHost_Id(user.getId(), host.getId())
        .orElseGet(() -> createNewChatRoom(user, host));

    return new ChatCreateResponse(chatRoom.getId());
  }

  /**
   * 일반회원이 채팅방 목록 조회
   */
  public PageResponse<ChatRoomResponse> getChatRoomsAsUser(Long userId, Pageable pageable) {
    Page<ChatRoom> chatRooms = chatRoomRepository.findAllByUser_Id(userId, pageable);
    Page<ChatRoomResponse> responses = chatRooms.map(this::createChatRoomResponseAsUser);
    return PageResponse.from(responses);
  }

  /**
   * 호스트가 채팅방 목록 조회
   */
  public PageResponse<ChatRoomResponse> getChatRoomsAsHost(Long hostId, Pageable pageable) {
    Page<ChatRoom> chatRooms = chatRoomRepository.findAllByHost_Id(hostId, pageable);
    Page<ChatRoomResponse> response = chatRooms.map(this::createChatRoomResponseAsHost);
    return PageResponse.from(response);
  }

  /**
   * 메시지 이력 조회
   */
  @Transactional
  public PageResponse<ChatMessageResponse> getChatMessages(
      Long viewerId,
      Long chatRoomId,
      Pageable pageable,
      SenderType senderType
  ) {
    ChatRoom chatRoom = findAndValidateChatRoom(viewerId, chatRoomId, senderType);
    updateReadStatus(chatRoom, viewerId, senderType);

    Page<ChatMessageResponse> responses = chatMessageRepository.findByChatRoomId(
        chatRoomId, pageable).map(ChatMessageResponse::from);

    return PageResponse.from(responses);
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
    ChatRoom chatRoom = findAndValidateChatRoom(senderId, chatRoomId, senderType);
    ChatMessage savedMessage = saveMessageAndUpdateChat(
        content, senderId, chatRoom, senderType, MessageType.MESSAGE);
    sendWebSocketMessage(chatRoomId, savedMessage);
  }

  /**
   * 사진 전송 및 저장
   */
  @Transactional
  public void sendImage(
      Long chatRoomId,
      MultipartFile imageFile,
      Long senderId,
      SenderType senderType
  ) {
    ChatRoom chatRoom = findAndValidateChatRoom(senderId, chatRoomId, senderType);
    String imageUrl = imageService.storeImage(imageFile);
    ChatMessage savedMessage = saveMessageAndUpdateChat(
        imageUrl, senderId, chatRoom, senderType, MessageType.IMAGE);
    sendWebSocketMessage(chatRoomId, savedMessage);
  }

  private ChatRoom createNewChatRoom(User user, Host host) {
    ChatRoom newChatRoom = createChatRoom(user, host);
    ChatRoom savedChatRoom = chatRoomRepository.save(newChatRoom);
    createReadStatusForParticipants(savedChatRoom, user.getId(), host.getId()); // 읽음 상태 초기화
    return savedChatRoom;
  }

  private ChatMessage saveMessageAndUpdateChat(
      String content,
      Long senderId,
      ChatRoom chatRoom,
      SenderType senderType,
      MessageType messageType
  ) {
    ChatMessage message = createChatMessage(chatRoom, content, senderType, messageType);
    ChatMessage savedMessage = chatMessageRepository.save(message);
    chatRoom.updateLastActivity(); // 채팅방 업데이트 시간 갱신
    updateReadStatus(chatRoom, senderId, senderType);
    return savedMessage;
  }

  private void updateReadStatus(ChatRoom chatRoom, Long participantId, SenderType senderType) {
    ChatReadStatus chatReadStatus = chatReadStatusRepository
        .findByChatRoomIdAndParticipantIdAndParticipantType(
            chatRoom.getId(), participantId, senderType)
        .orElseGet(() -> {
              ChatReadStatus newChatReadStatus = createChatReadStatus(
                  chatRoom, participantId, senderType);
              return chatReadStatusRepository.save(newChatReadStatus);
            }
        );
    chatReadStatus.updateLastReadTime(LocalDateTime.now());
  }

  private void sendWebSocketMessage(Long chatRoomId, ChatMessage savedMessage) {
    ChatMessageResponse payload = ChatMessageResponse.from(savedMessage);
    messagingTemplate.convertAndSend(CHAT_DESTINATION + chatRoomId, payload);
    log.debug("메시지 전송 완료: {}", CHAT_DESTINATION + chatRoomId);
  }

  private ChatRoom findAndValidateChatRoom(Long senderId, Long chatRoomId, SenderType senderType) {
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

  private ChatReadStatus createChatReadStatus(
      ChatRoom chatRoom, Long participantId, SenderType participantType
  ) {
    return ChatReadStatus.builder()
        .chatRoom(chatRoom)
        .participantId(participantId)
        .participantType(participantType)
        .lastReadTime(LocalDateTime.now())
        .build();
  }

  private ChatRoom createChatRoom(User user, Host host) {
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

    int unreadCount = chatMessageRepository.countByChatRoomIdAndSenderTypeAndCreatedAtGreaterThan(
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
        getLastMessageType(lastMessage),
        getLastMessageTime(lastMessage),
        unreadCount
    );
  }

  private ChatRoomResponse createChatRoomResponseAsHost(ChatRoom chatRoom) {
    ChatMessage lastMessage = chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(
        chatRoom.getId()); // 마지막 메시지 정보

    User user = chatRoom.getUser();
    int unreadCount = chatMessageRepository.countByChatRoomIdAndSenderTypeAndCreatedAtGreaterThan(
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
        getLastMessageType(lastMessage),
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

  private String getLastMessage(ChatMessage lastMessage) {
    return lastMessage != null ? lastMessage.getContent() : "";
  }

  private MessageType getLastMessageType(ChatMessage lastMessage) {
    return lastMessage != null ? lastMessage.getMessageType() : MessageType.MESSAGE;
  }

  private LocalDateTime getLastMessageTime(ChatMessage lastMessage) {
    return lastMessage != null ? lastMessage.getCreatedAt() : null;
  }

  private ChatMessage createChatMessage(
      ChatRoom chatRoom,
      String content,
      SenderType senderType,
      MessageType messageType
  ) {
    return ChatMessage.builder()
        .chatRoom(chatRoom)
        .content(content)
        .senderType(senderType)
        .messageType(messageType)
        .build();
  }
}
