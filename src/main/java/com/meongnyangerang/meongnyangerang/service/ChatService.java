package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.chat.ChatMessage;
import com.meongnyangerang.meongnyangerang.domain.chat.ChatReadStatus;
import com.meongnyangerang.meongnyangerang.domain.chat.ChatRoom;
import com.meongnyangerang.meongnyangerang.domain.chat.MessageType;
import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatMessageResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatCreateResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatMessageHistoryResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatMessageAndReceiverInfoResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatRoomResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatRoomPartnerInfo;
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
   * 일반회원 채팅방 목록 조회
   */
  public PageResponse<ChatRoomResponse> getChatRoomsAsUser(Long viewerId, Pageable pageable) {
    Page<ChatRoom> chatRooms = chatRoomRepository.findAllByUser_Id(viewerId, pageable);
    Page<ChatRoomResponse> responses = chatRooms.map(
        chatRoom -> createChatRoomResponse(viewerId, chatRoom, SenderType.USER));

    return PageResponse.from(responses);
  }

  /**
   * 호스트 채팅방 목록 조회
   */
  public PageResponse<ChatRoomResponse> getChatRoomsAsHost(Long viewerId, Pageable pageable) {
    Page<ChatRoom> chatRooms = chatRoomRepository.findAllByHost_Id(viewerId, pageable);
    Page<ChatRoomResponse> responses = chatRooms.map(
        chatRoom -> createChatRoomResponse(viewerId, chatRoom, SenderType.HOST));

    return PageResponse.from(responses);
  }

  /**
   * 메시지 이력 조회
   */
  @Transactional
  public ChatMessageHistoryResponse getChatMessages(
      Long viewerId,
      Long chatRoomId,
      Pageable pageable,
      SenderType viewerType
  ) {
    ChatRoom chatRoom = findAndValidateChatRoom(viewerId, chatRoomId, viewerType);
    updateReadStatus(chatRoom, viewerId, viewerType);

    Page<ChatMessageResponse> responses = chatMessageRepository.findByChatRoomId(
        chatRoomId, pageable).map(ChatMessageResponse::from);
    ChatRoomPartnerInfo chatRoomPartnerInfo = getChatRoomPartnerInfo(chatRoom, viewerType);

    return ChatMessageHistoryResponse.of(
        PageResponse.from(responses),
        chatRoomPartnerInfo.partnerName(),
        chatRoomPartnerInfo.partnerImageUrl()
    );
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

    sendWebSocketMessageWithSenderInfo(chatRoomId, senderType, savedMessage, chatRoom);
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
    sendWebSocketMessageWithSenderInfo(chatRoomId, senderType, savedMessage, chatRoom);
  }

  private void sendWebSocketMessageWithSenderInfo(
      Long chatRoomId,
      SenderType senderType,
      ChatMessage savedMessage,
      ChatRoom chatRoom
  ) {
    ChatRoomPartnerInfo chatRoomPartnerInfo = getChatRoomMyInfo(chatRoom, senderType);
    sendWebSocketMessage(
        chatRoomId,
        savedMessage,
        chatRoomPartnerInfo.partnerName(),
        chatRoomPartnerInfo.partnerImageUrl()
    );
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

  private void sendWebSocketMessage(
      Long chatRoomId,
      ChatMessage savedMessage,
      String receiverName,
      String receiverImageUrl
  ) {
    ChatMessageAndReceiverInfoResponse payload = ChatMessageAndReceiverInfoResponse.from(
        savedMessage, receiverName, receiverImageUrl);
    messagingTemplate.convertAndSend(CHAT_DESTINATION + chatRoomId, payload);
  }

  private ChatRoom findAndValidateChatRoom(Long viewerId, Long chatRoomId, SenderType senderType) {
    ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.NOT_EXIST_CHAT_ROOM));
    senderType.validateAccess(chatRoom, viewerId); // 발신자가 채팅방 참여자인지 확인
    return chatRoom;
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

  private ChatRoomResponse createChatRoomResponse(
      Long viewerId, ChatRoom chatRoom, SenderType viewerType
  ) {
    ChatMessage lastMessage = chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(
        chatRoom.getId()); // 마지막 메시지 정보
    ChatRoomPartnerInfo chatRoomPartnerInfo = getChatRoomPartnerInfo(chatRoom, viewerType);

    int unreadCount = chatMessageRepository.countByChatRoomIdAndSenderTypeAndCreatedAtGreaterThan(
        chatRoom.getId(),
        chatRoomPartnerInfo.partnerType(),
        getLastReadTime(chatRoom.getId(), viewerId, viewerType)
    );

    return new ChatRoomResponse(
        chatRoom.getId(),
        chatRoomPartnerInfo.partnerId(),
        chatRoomPartnerInfo.partnerName(),
        chatRoomPartnerInfo.partnerImageUrl(),
        getLastMessage(lastMessage),
        getLastMessageType(lastMessage),
        getLastMessageTime(lastMessage),
        unreadCount
    );
  }

  private ChatRoomPartnerInfo getChatRoomMyInfo(ChatRoom chatRoom, SenderType viewerType) {
    if (viewerType == SenderType.USER) {
      User user = chatRoom.getUser();
      return getChatRoomUserInfo(user);
    } else if (viewerType == SenderType.HOST) {
      return getChatRoomHostInfo(chatRoom.getHostId());
    }
    throw new MeongnyangerangException(ErrorCode.INVALID_AUTHORIZED);
  }

  private ChatRoomPartnerInfo getChatRoomPartnerInfo(ChatRoom chatRoom, SenderType viewerType) {
    if (viewerType == SenderType.USER) {
      return getChatRoomHostInfo(chatRoom.getHostId());
    } else if (viewerType == SenderType.HOST) {
      User user = chatRoom.getUser();
      return getChatRoomUserInfo(user);
    }
    throw new MeongnyangerangException(ErrorCode.INVALID_AUTHORIZED);
  }

  private ChatRoomPartnerInfo getChatRoomHostInfo(Long hostId) {
    Accommodation accommodation = getAccommodationByHostId(hostId);
    return new ChatRoomPartnerInfo(
        hostId,
        accommodation.getName(),
        accommodation.getThumbnailUrl(),
        SenderType.HOST
    );
  }

  private static ChatRoomPartnerInfo getChatRoomUserInfo(User user) {
    return new ChatRoomPartnerInfo(
        user.getId(),
        user.getNickname(),
        user.getProfileImage(),
        SenderType.USER
    );
  }

  private Accommodation getAccommodationByHostId(Long hostId) {
    return accommodationRepository.findByHostId(hostId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.ACCOMMODATION_NOT_FOUND));
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
}
