package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.domain.chat.SenderType;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatCreateRequest;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatCreateResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatMessageHistoryResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.ChatRoomResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.SendImageRequest;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chats")
public class ChatController {

  private final ChatService chatService;

  /**
   * 채팅방 생성 API (채팅방이 이미 존재하면 존재하는 채팅방의 ID 반환)
   */
  @PostMapping("users/create")
  public ResponseEntity<ChatCreateResponse> createChatRoom(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @RequestBody ChatCreateRequest request
  ) {
    return ResponseEntity.ok(
        chatService.createChatRoom(userDetails.getId(), request.accommodationId()));
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
    SenderType viewerType = userDetails.getRole().toSenderType();
    return ResponseEntity.ok(viewerType.getChatRooms(chatService, userDetails.getId(), pageable));
  }

  /**
   * 메시지 이력 조회
   */
  @GetMapping("/{chatRoomId}/messages")
  public ResponseEntity<ChatMessageHistoryResponse> getChatMessages(
      @PathVariable Long chatRoomId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
      Pageable pageable,
      @AuthenticationPrincipal UserDetailsImpl userDetails
  ) {
    return ResponseEntity.ok(chatService.getChatMessages(
        userDetails.getId(), chatRoomId, pageable, userDetails.getRole().toSenderType()));
  }

  /**
   * 사진 전송
   */
  @PostMapping("/send/image")
  public ResponseEntity<Void> sendImage(
      @RequestPart SendImageRequest request,
      @RequestPart MultipartFile imageFile,
      @AuthenticationPrincipal UserDetailsImpl userDetails
  ) {
    chatService.sendImage(
        request.chatRoomId(), imageFile, userDetails.getId(), userDetails.getRole().toSenderType());
    return ResponseEntity.ok().build();
  }
}
