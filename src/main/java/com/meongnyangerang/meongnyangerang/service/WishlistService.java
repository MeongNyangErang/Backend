package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ACCOMMODATION_NOT_FOUND;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ALREADY_WISHLISTED;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXIST_ACCOUNT;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.Wishlist;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.repository.WishlistRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
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
}
