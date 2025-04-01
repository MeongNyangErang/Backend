package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.UserPetRequest;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.UserPetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/pets")
public class UserPetController {

  private final UserPetService userPetService;

  // 사용자 반려동물 등록 API
  @PostMapping
  public ResponseEntity<Void> registerPet(@AuthenticationPrincipal UserDetailsImpl userDetails,
      @Valid @RequestBody UserPetRequest request) {

    userPetService.registerPet(userDetails.getId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  // 사용자 반려동물 수정 API
  @PutMapping("/{petId}")
  public ResponseEntity<Void> updatePet(@AuthenticationPrincipal UserDetailsImpl userDetails,
      @PathVariable Long petId, @Valid @RequestBody UserPetRequest request) {
    userPetService.updatePet(userDetails.getId(), petId, request);
    return ResponseEntity.ok().build();
  }
}
