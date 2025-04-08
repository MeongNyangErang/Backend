package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatRoomResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatCreateRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chats")
public class ChatController {

  private final ChatService chatService;

  /**
   * 채팅 시작
   */
  @PostMapping("users/create")
  public ResponseEntity<Void> createChatRoom(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @RequestBody ChatCreateRequest request
  ) {
    chatService.createChatRoom(userDetails.getId(), request.accommodationId());
    return ResponseEntity.ok().build();
  }

  /**
   * 채팅방 목록 조회
   */
  @GetMapping
  public ResponseEntity<PageResponse<ChatRoomResponse>> getChatRooms(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC)
      Pageable pageable
  ) {
    Role viewerRole = userDetails.getRole();

    if (viewerRole == Role.ROLE_USER) {
      return ResponseEntity.ok(chatService.getChatRoomsAsUser(userDetails.getId(), pageable));
    } else if (viewerRole == Role.ROLE_HOST) {
      return ResponseEntity.ok(chatService.getChatRoomsAsHost(userDetails.getId(), pageable));
    } else {
      throw new MeongnyangerangException(ErrorCode.INVALID_AUTHORIZED);
    }
  }
}
