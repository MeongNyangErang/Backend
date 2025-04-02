package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ACCOMMODATION_NOT_FOUND;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ALREADY_WISHLISTED;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXIST_ACCOUNT;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXIST_WISHLIST;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.Wishlist;
import com.meongnyangerang.meongnyangerang.dto.CustomWishlistResponse;
import com.meongnyangerang.meongnyangerang.dto.WishlistResponse;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.ReviewRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.repository.WishlistRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistService {

  private final AccommodationRepository accommodationRepository;
  private final WishlistRepository wishlistRepository;
  private final UserRepository userRepository;

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
  public CustomWishlistResponse<WishlistResponse> getUserWishlists(Long userId, Long cursorId,
      int size) {

    // size + 1개 조회해서 hasNext 판단
    List<Wishlist> wishlists = wishlistRepository.findByUserId(userId, cursorId, size + 1);

    boolean hasNext = wishlists.size() > size;
    Long nextCursor = hasNext ? wishlists.get(size).getId() : null;

    // 현재 페이지 찜 목록만 사용
    List<WishlistResponse> content = wishlists.stream()
        .limit(size)
        .map(wishlist -> {
          Accommodation accommodation = wishlist.getAccommodation();

          return WishlistResponse.builder()
              .wishlistId(wishlist.getId())
              .accommodationId(accommodation.getId())
              .accommodationName(accommodation.getName())
              .thumbnailImageUrl(accommodation.getThumbnailUrl())
              .address(accommodation.getAddress())
//              .totalRating(accommodation.getTotalRating())   // 추후 숙소 엔티티에 totalRating 필드가 생성되면 수정 예정
              .build();
        })
        .toList();

    return new CustomWishlistResponse<>(content, nextCursor, hasNext);
  }
}
