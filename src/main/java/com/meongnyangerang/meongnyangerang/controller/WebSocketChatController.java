package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.dto.chat.SendMessageRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.service.ChatService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketChatController {

  private final ChatService chatService;

  /**
   * 메시지 전송
   */
  @MessageMapping("/chats/send/{chatRoomId}")
  public void handleMessage(
      @DestinationVariable Long chatRoomId,
      @Payload SendMessageRequest request,
      Principal principal
  ) {
    String[] parts = getSplitUserKey(principal.getName());
    SenderType senderType = SenderType.valueOf(parts[0]);
    Long senderId = Long.parseLong(parts[1]);

    chatService.sendMessage(chatRoomId, request.content(), senderId, senderType);
  }

  private String[] getSplitUserKey(String userKey) {
    String[] parts = userKey.split("_");
    if (parts.length != 2) {
      throw new MeongnyangerangException(ErrorCode.WEBSOCKET_NOT_AUTHORIZED);
    }
    return parts;
  }
}
