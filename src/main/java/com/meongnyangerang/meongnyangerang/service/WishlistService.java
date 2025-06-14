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
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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

    // DB에 저장
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

    // Redis에서 제거
    redisTemplate.opsForSet().remove(getWishlistKey(userId), accommodationId);

    // DB에서 삭제
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

  // 찜 여부 확인
  public boolean isWishlisted(Long userId, Long accommodationId) {
    return Boolean.TRUE.equals(
        redisTemplate.opsForSet().isMember(getWishlistKey(userId), accommodationId));
  }

  // Redis에서 해당 사용자가 찜한 숙소 ID 목록을 조회
  public Set<Long> getWishlistIdsFromRedis(Long userId) {
    if (userId == null) {
      return Collections.emptySet();
    }

    String key = getWishlistKey(userId);
    Set<Long> redisIds = redisTemplate.opsForSet().members(key);

    return redisIds != null ? redisIds : Collections.emptySet();
  }

  /**
   * 서버 시작 시 DB에 저장된 찜 정보를 Redis에 로딩합니다.
   * Redis가 비어 있는 경우를 대비하여 일관성을 유지합니다.(서버 재시작 등)
   */
  @PostConstruct
  public void preloadWishlistToRedis() {
    List<Wishlist> all = wishlistRepository.findAll();
    for (Wishlist w : all) {
      String key = getWishlistKey(w.getUser().getId());
      redisTemplate.opsForSet().add(key, w.getAccommodation().getId());
    }
  }
}
