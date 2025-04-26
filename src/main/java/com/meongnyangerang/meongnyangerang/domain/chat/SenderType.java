package com.meongnyangerang.meongnyangerang.domain.chat;

import com.meongnyangerang.meongnyangerang.dto.chat.ChatRoomResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.service.ChatService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

@Getter
@RequiredArgsConstructor
public enum SenderType {

  USER("일반 회원") {
    @Override
    public PageResponse<ChatRoomResponse> getChatRooms(
        ChatService chatService, Long userId, Pageable pageable) {
      return chatService.getChatRoomsAsUser(userId, pageable);
    }

    @Override
    protected boolean checkAccess(ChatRoom chatRoom, Long viewerId) {
      return chatRoom.getUser().getId().equals(viewerId);
    }
  },

  HOST("호스트 회원") {
    @Override
    public PageResponse<ChatRoomResponse> getChatRooms(
        ChatService chatService, Long HostId, Pageable pageable) {
      return chatService.getChatRoomsAsHost(HostId, pageable);
    }

    @Override
    protected boolean checkAccess(ChatRoom chatRoom, Long viewerId) {
      return chatRoom.getHost().getId().equals(viewerId);
    }
  };

  private final String value;

  public abstract PageResponse<ChatRoomResponse> getChatRooms(
      ChatService chatService, Long id, Pageable pageable);

  protected abstract boolean checkAccess(ChatRoom chatRoom, Long viewerId);

  public void validateAccess(ChatRoom chatRoom, Long viewerId) {
    if (!checkAccess(chatRoom, viewerId)) {
      throw new MeongnyangerangException(ErrorCode.CHAT_ROOM_NOT_AUTHORIZED);
    }
  }
}
