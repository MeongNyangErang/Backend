package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.CustomWishlistResponse;
import com.meongnyangerang.meongnyangerang.dto.WishlistResponse;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  // 찜 삭제 API
  @DeleteMapping("/{accommodationId}")
  public ResponseEntity<Void> removeWishlist(@AuthenticationPrincipal UserDetailsImpl userDetails,
      @PathVariable Long accommodationId) {
    wishlistService.removeWishlist(userDetails.getId(), accommodationId);
    return ResponseEntity.noContent().build();
  }

  // 찜 조회 API
  @GetMapping
  public ResponseEntity<CustomWishlistResponse<WishlistResponse>> getUserWishlist(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @RequestParam(defaultValue = "0") Long cursor,
      @RequestParam(defaultValue = "20") @Range(min = 1, max = 100) int size) {

    return ResponseEntity.ok(wishlistService.getUserWishlists(userDetails.getId(), cursor, size));
  }

}
