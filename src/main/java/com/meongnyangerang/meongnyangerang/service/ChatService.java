package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.chat.ChatMessage;
import com.meongnyangerang.meongnyangerang.domain.chat.ChatReadStatus;
import com.meongnyangerang.meongnyangerang.domain.chat.ChatRoom;
import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatRoomResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatMessageRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatReadStatusRepository;
import com.meongnyangerang.meongnyangerang.repository.chat.ChatRoomRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatReadStatusRepository chatReadStatusRepository;
  private final ChatMessageRepository chatMessageRepository;

  private static final LocalDateTime DEFAULT_LAST_READ_TIME =
      LocalDateTime.of(2000, 1, 1, 0, 0);

  /**
   * 채팅방 목록 조회
   */
  public List<ChatRoomResponse> getChatRooms(Long viewerId, Role viewerRole) {
    List<ChatRoom> chatRooms = findChatRoomsByViewer(viewerId, viewerRole);

    return chatRooms.stream()
        .map(chatRoom -> {
          ChatMessage lastMessage = chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(
              chatRoom.getId()); // 마지막 메시지 정보
          int unreadCount = calculateUnreadCount(chatRoom, viewerRole); // 읽지 않은 메시지 수
          return ChatRoomResponse.of(chatRoom, lastMessage, unreadCount, viewerRole);
        })
        .toList();
  }

  /**
   * 참여자 역할에 따른 채팅방 목록 조회
   */
  private List<ChatRoom> findChatRoomsByViewer(Long viewerId, Role viewerRole) {
    if (viewerRole == Role.ROLE_USER) {
      return chatRoomRepository.findAllByUserIdOrderByUpdatedAtDesc(viewerId);
    } else if (viewerRole == Role.ROLE_HOST) {
      return chatRoomRepository.findAllByHostIdOrderByUpdatedAtDesc(viewerId);
    } else {
      throw new MeongnyangerangException(ErrorCode.INVALID_AUTHORIZED);
    }
  }

  private LocalDateTime getLastReadTime(
      Long roomId,
      Long participantId,
      SenderType viewerType
  ) {
    return chatReadStatusRepository
        .findByChatRoomIdAndParticipantIdAndParticipantType(roomId, participantId, viewerType)
        .map(ChatReadStatus::getLastReadTime)
        .orElse(DEFAULT_LAST_READ_TIME);
  }

  /**
   * 읽지 않은 메시지 수 계산
   */
  private int calculateUnreadCount(ChatRoom chatRoom, Role viewerRole) {
    if (viewerRole == Role.ROLE_USER) {
      return countUnreadMessagesForUser(chatRoom);
    } else if (viewerRole == Role.ROLE_HOST) {
      return countUnreadMessagesForHost(chatRoom);
    } else {
      throw new MeongnyangerangException(ErrorCode.INVALID_AUTHORIZED);
    }
  }

  /**
   * 사용자 관점에서 읽지 않은 메시지 수 계산
   */
  private int countUnreadMessagesForUser(ChatRoom chatRoom) {
    Long userId = chatRoom.getUser().getId();

    return chatMessageRepository.countUnreadMessages(
        chatRoom.getId(),
        SenderType.HOST,  // 호스트가 보낸 메시지 중에서
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
        SenderType.USER,  // 사용자가 보낸 메시지 중에서
        getLastReadTime(chatRoom.getId(), hostId, SenderType.HOST)
    );
  }
}
