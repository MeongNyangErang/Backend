package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.room.RoomCreateRequest;
import com.meongnyangerang.meongnyangerang.dto.room.RoomListResponse;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/hosts/rooms")
public class RoomController {

  private final RoomService roomService;

  /**
   * 객실 생성
   */
  @PostMapping
  public ResponseEntity<Void> createRoom(
      @AuthenticationPrincipal UserDetailsImpl userDetail,
      @Valid @RequestPart RoomCreateRequest request,
      @RequestPart MultipartFile image
  ) {
    roomService.createRoom(userDetail.getId(), request, image);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /**
   * 객실 목록 조회
   */
  @GetMapping("/{accommodationId}")
  public ResponseEntity<RoomListResponse> getRoomList(
      @AuthenticationPrincipal UserDetailsImpl userDetail,
      @PathVariable Long accommodationId,
      @RequestParam(required = false) Long cursorId,
      @RequestParam(defaultValue = "20") @Range(min = 1, max = 100) int pageSize
  ) {
    return ResponseEntity.ok(
        roomService.getRoomList(userDetail.getId(), accommodationId, cursorId, pageSize));
  }
}
