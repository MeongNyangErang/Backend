package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.room.RoomCreateRequest;
import com.meongnyangerang.meongnyangerang.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
      @Valid @RequestPart RoomCreateRequest request,
      @RequestPart MultipartFile image
  ) {
    roomService.createRoom(request, image);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}
