package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ACCOMMODATION_NOT_FOUND;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ALREADY_WISHLISTED;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXIST_ACCOUNT;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXIST_WISHLIST;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.Wishlist;
import com.meongnyangerang.meongnyangerang.dto.WishlistResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.repository.WishlistRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistService {

  private final AccommodationRepository accommodationRepository;
  private final WishlistRepository wishlistRepository;
  private final UserRepository userRepository;
  private final RedisTemplate<String, Long> redisTemplate;

  private String getWishlistKey(Long userId) {
    return "wishlist:" + userId;
  }

  // 찜 등록
  @Transactional

  public void addWishlist(Long userId, Long accommodationId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    Accommodation accommodation = accommodationRepository.findById(accommodationId)
        .orElseThrow(() -> new MeongnyangerangException(ACCOMMODATION_NOT_FOUND));

    if (wishlistRepository.existsByUserIdAndAccommodationId(userId, accommodationId)) {
      throw new MeongnyangerangException(ALREADY_WISHLISTED);
    }

    // Redis에 추가 저장
    redisTemplate.opsForSet().add(getWishlistKey(userId), accommodationId);

    wishlistRepository.save(Wishlist.builder()
        .user(user)
        .accommodation(accommodation)
        .build());
  }

  // 찜 삭제
  @Transactional
  public void removeWishlist(Long userId, Long accommodationId) {
    Wishlist wishlist = wishlistRepository.findByUserIdAndAccommodationId(userId, accommodationId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_WISHLIST));

    wishlistRepository.delete(wishlist);
  }

  // 찜 목록 조회
  public PageResponse<WishlistResponse> getUserWishlists(Long userId, Pageable pageable) {

    Page<Wishlist> wishlists = wishlistRepository.findByUserId(userId, pageable);

    Page<WishlistResponse> responsePage = wishlists.map(wishlist -> {
      Accommodation accommodation = wishlist.getAccommodation();
      return WishlistResponse.builder()
          .wishlistId(wishlist.getId())
          .accommodationId(accommodation.getId())
          .accommodationName(accommodation.getName())
          .thumbnailImageUrl(accommodation.getThumbnailUrl())
          .address(accommodation.getAddress())
          .totalRating(accommodation.getTotalRating())
          .build();
    });

    return PageResponse.from(responsePage);
  }
}
