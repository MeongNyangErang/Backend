package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.room.RoomCreateRequest;
import com.meongnyangerang.meongnyangerang.dto.room.RoomListResponse;
import com.meongnyangerang.meongnyangerang.dto.room.RoomResponse;
import com.meongnyangerang.meongnyangerang.dto.room.RoomUpdateRequest;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
      @RequestPart MultipartFile thumbnail
  ) {
    roomService.createRoom(userDetail.getId(), request, thumbnail);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /**
   * 객실 목록 조회
   */
  @GetMapping
  public ResponseEntity<RoomListResponse> getRoomList(
      @AuthenticationPrincipal UserDetailsImpl userDetail,
      @RequestParam(required = false) Long cursorId,
      @RequestParam(defaultValue = "20") @Range(min = 1, max = 100) int size
  ) {
    return ResponseEntity.ok(roomService.getRoomList(userDetail.getId(), cursorId, size));
  }

  /**
   * 객실 상세 조회
   */
  @GetMapping("/{roomId}")
  public ResponseEntity<RoomResponse> getRoom(
      @AuthenticationPrincipal UserDetailsImpl userDetail,
      @PathVariable Long roomId
  ) {
    return ResponseEntity.ok(roomService.getRoom(userDetail.getId(), roomId));
  }

  /**
   * 객실 수정
   */
  @PutMapping
  public ResponseEntity<RoomResponse> updateRoom(
      @AuthenticationPrincipal UserDetailsImpl userDetail,
      @Valid @RequestPart RoomUpdateRequest request,
      @RequestPart(required = false) MultipartFile newThumbnail
  ) {
    return ResponseEntity.ok(roomService.updateRoom(userDetail.getId(), request, newThumbnail));
  }

  /**
   * 객실 삭제
   */
  @DeleteMapping("/{roomId}")
  public ResponseEntity<Void> deleteRoom(
      @AuthenticationPrincipal UserDetailsImpl userDetail,
      @PathVariable Long roomId
  ){
    roomService.deleteRoom(userDetail.getId(), roomId);
    return ResponseEntity.noContent().build();
  }
}
