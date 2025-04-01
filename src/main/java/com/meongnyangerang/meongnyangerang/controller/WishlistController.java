package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/wishlist/accommodations")
public class WishlistController {

  private final WishlistService wishlistService;

  // 찜 등록 API
  @PostMapping("/{accommodationId}")
  public ResponseEntity<Void> addWishlist(@AuthenticationPrincipal UserDetailsImpl userDetails,
      @PathVariable Long accommodationId) {
    wishlistService.addWishlist(userDetails.getId(), accommodationId);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

}
