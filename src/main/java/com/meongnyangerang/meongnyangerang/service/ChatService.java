package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.chat.ChatMessage;
import com.meongnyangerang.meongnyangerang.domain.chat.ChatReadStatus;
import com.meongnyangerang.meongnyangerang.domain.chat.ChatRoom;
import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatRoomResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatMessageRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatReadStatusRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatRoomRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatReadStatusRepository chatReadStatusRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final AccommodationRepository accommodationRepository;

  private static final LocalDateTime DEFAULT_LAST_READ_TIME =
      LocalDateTime.of(2000, 1, 1, 0, 0);

  /**
   * 일반회원이 채팅방 목록 조회
   */
  public PageResponse<ChatRoomResponse> getChatRoomsAsUser(Long userId, Pageable pageable) {
    log.info("일반회원 채팅방 목록 조회");
    
    Page<ChatRoom> chatRooms = chatRoomRepository.findAllByUser_IdOrderByUpdatedAtDesc(
        userId, pageable);
    Page<ChatRoomResponse> response = chatRooms.map(this::createChatRoomResponseAsUser);

    return PageResponse.from(response);
  }

  /**
   * 호스트가 채팅방 목록 조회
   */
  public PageResponse<ChatRoomResponse> getChatRoomsAsHost(Long hostId, Pageable pageable) {
    log.info("호스트 채팅방 목록 조회");

    Page<ChatRoom> chatRooms = chatRoomRepository.findAllByHost_IdOrderByUpdatedAtDesc(
        hostId, pageable);

    Page<ChatRoomResponse> response = chatRooms.map(this::createChatRoomResponseAsHost);

    return PageResponse.from(response);
  }

  private ChatRoomResponse createChatRoomResponseAsUser(ChatRoom chatRoom) {
    Accommodation accommodation = accommodationRepository.findByHostId(chatRoom.getHostId())
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.ACCOMMODATION_NOT_FOUND));

    ChatMessage lastMessage = chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(
        chatRoom.getId()); // 마지막 메시지 정보

    int unreadCount = countUnreadMessagesForUser(chatRoom); // 읽지 않은 메시지 수

    return ChatRoomResponse.createChatRoomResponse(
        chatRoom.getId(),
        chatRoom.getHostId(),
        accommodation.getName(),
        accommodation.getThumbnailUrl(),
        lastMessage.getContent(),
        lastMessage.getCreatedAt(),
        unreadCount
    );
  }

  private ChatRoomResponse createChatRoomResponseAsHost(ChatRoom chatRoom) {
    ChatMessage lastMessage = chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(
        chatRoom.getId()); // 마지막 메시지 정보

    User user = chatRoom.getUser();
    int unreadCount = countUnreadMessagesForHost(chatRoom); // 읽지 않은 메시지 수

    return ChatRoomResponse.createChatRoomResponse(
        chatRoom.getId(),
        user.getId(),
        user.getNickname(),
        user.getProfileImage(),
        lastMessage.getContent(),
        lastMessage.getCreatedAt(),
        unreadCount
    );
  }

  /**
   * 사용자 관점에서 읽지 않은 메시지 수 계산
   */
  private int countUnreadMessagesForUser(ChatRoom chatRoom) {
    Long userId = chatRoom.getUser().getId();

    return chatMessageRepository.countUnreadMessages(
        chatRoom.getId(),
        SenderType.HOST, // 호스트가 보낸 메시지 중에서
        getLastReadTime(chatRoom.getId(), userId, SenderType.USER)
    );
  }

  /**
   * 호스트 관점에서 읽지 않은 메시지 수 계산
   */
  private int countUnreadMessagesForHost(ChatRoom chatRoom) {
    Long hostId = chatRoom.getHost().getId();

    return chatMessageRepository.countUnreadMessages(
        chatRoom.getId(),
        SenderType.USER, // 사용자가 보낸 메시지 중에서
        getLastReadTime(chatRoom.getId(), hostId, SenderType.HOST)
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
}
