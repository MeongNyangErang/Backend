package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.room.RoomResponse;
import com.meongnyangerang.meongnyangerang.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
public class PublicRoomController {

  private final RoomService roomService;

  // 객실 상세 조회 API
  @GetMapping("/{roomId}")
  public ResponseEntity<RoomResponse> getRoomDetail(@PathVariable Long roomId) {
    RoomResponse response = roomService.getRoomDetail(roomId);
    return ResponseEntity.ok(response);
  }
}
