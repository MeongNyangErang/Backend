package com.meongnyangerang.meongnyangerang.dto.chat;

import com.meongnyangerang.meongnyangerang.domain.chat.ChatMessage;
import com.meongnyangerang.meongnyangerang.domain.chat.ChatRoom;
import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import java.time.LocalDateTime;

public record ChatRoomResponse(
    Long chatroomId,
    Long partnerId,
    String viewerNickname,
    String partnerNickname,
    String partnerProfileImageUrl,
    String lastMessage,
    LocalDateTime lastMessageTime,
    int unreadCount // 현재 보는 사람 기준 읽지 않은 메시지 수
) {

  /**
   * 역할에 따른 ChatRoomResponse 생성
   */
  public static ChatRoomResponse of(
      ChatRoom chatRoom,
      ChatMessage lastMessage,
      int unreadCount,
      Role viewerRole
  ) {
    if (viewerRole == Role.ROLE_USER) {
      return createUserViewResponse(chatRoom, lastMessage, unreadCount);
    } else if (viewerRole == Role.ROLE_HOST) {
      return createHostViewResponse(chatRoom, lastMessage, unreadCount);
    } else {
      throw new MeongnyangerangException(ErrorCode.INVALID_AUTHORIZED);
    }
  }

  /**
   * 사용자 관점의 응답 생성
   */
  private static ChatRoomResponse createUserViewResponse(
      ChatRoom chatRoom,
      ChatMessage lastMessage,
      int unreadCount
  ) {
    return new ChatRoomResponse(
        chatRoom.getId(),
        chatRoom.getHost().getId(),              // 상대방 ID (호스트)
        chatRoom.getUser().getNickname(),        // 조회자 이름 (사용자)
        chatRoom.getHost().getNickname(),        // 상대방 이름 (호스트)
        chatRoom.getHost().getProfileImageUrl(), // 상대방의 프로필 사진 URL (호스트)
        lastMessage != null ? lastMessage.getContent() : "",
        lastMessage != null ? lastMessage.getCreatedAt() : null,
        unreadCount
    );
  }

  /**
   * 호스트 관점의 응답 생성
   */
  private static ChatRoomResponse createHostViewResponse(
      ChatRoom chatRoom,
      ChatMessage lastMessage,
      int unreadCount
  ) {
    return new ChatRoomResponse(
        chatRoom.getId(),
        chatRoom.getUser().getId(),           // 상대방 ID (사용자)
        chatRoom.getHost().getNickname(),     // 조회자 이름 (호스트)
        chatRoom.getUser().getNickname(),     // 상대방 이름 (사용자)
        chatRoom.getUser().getProfileImage(), // 상대방의 프로필 사진 URL (호스트)
        lastMessage != null ? lastMessage.getContent() : "",
        lastMessage != null ? lastMessage.getCreatedAt() : null,
        unreadCount
    );
  }
}
