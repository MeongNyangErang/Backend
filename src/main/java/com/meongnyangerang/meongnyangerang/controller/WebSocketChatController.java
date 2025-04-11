package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.dto.chat.SendMessageRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.ChatService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketChatController {

  private final ChatService chatService;

  /**
   * WebSocket 통한 메시지 전송 클라이언트가/app/chat/send/{chatRoomId}로 메시지를 보내면 처리
   */
  @MessageMapping("/chat/send/{chatRoomId}")
  public void handleMessage(
      @DestinationVariable Long chatRoomId,
      @Payload SendMessageRequest request,
      Principal principal
  ) {
    Authentication auth = (Authentication) principal;
    UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

    if (userDetails.getRole() == Role.ROLE_USER) {
      chatService.sendMessage(chatRoomId, request.content(), userDetails.getId(), SenderType.USER);
    } else if (userDetails.getRole() == Role.ROLE_HOST) {
      chatService.sendMessage(chatRoomId, request.content(), userDetails.getId(), SenderType.HOST);
    } else {
      throw new MeongnyangerangException(ErrorCode.INVALID_AUTHORIZED);
    }
  }
}
