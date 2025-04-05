package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.chat.ChatRoomResponse;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.ChatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chats")
public class ChatController {

  private final ChatService chatService;

  /**
   * 채팅방 목록 조회
   */
  @GetMapping("/rooms")
  public ResponseEntity<List<ChatRoomResponse>> getChatRooms(
      @AuthenticationPrincipal UserDetailsImpl userDetails
  ) {
    return ResponseEntity.ok(chatService.getChatRooms(userDetails.getId(), userDetails.getRole()));
  }
}
