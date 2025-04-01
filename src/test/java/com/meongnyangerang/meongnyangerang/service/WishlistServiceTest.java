package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.Wishlist;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.repository.WishlistRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

  @InjectMocks
  private WishlistService wishlistService;

  @Mock
  private WishlistRepository wishlistRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private AccommodationRepository accommodationRepository;

  @Test
  @DisplayName("찜 등록 성공")
  void addWishlistSuccess() {
    // given
    Long userId = 1L;
    Long accommodationId = 100L;
    User user = User.builder().id(userId).email("user@example.com").build();
    Accommodation accommodation = Accommodation.builder().id(accommodationId).name("테스트숙소").build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(accommodationRepository.findById(100L)).thenReturn(Optional.of(accommodation));
    when(wishlistRepository.existsByUserIdAndAccommodationId(1L, 100L)).thenReturn(false);

    // when
    assertDoesNotThrow(() -> wishlistService.addWishlist(1L, 100L));

    // then
    ArgumentCaptor<Wishlist> captor = ArgumentCaptor.forClass(Wishlist.class);
    verify(wishlistRepository).save(captor.capture());

    Wishlist saved = captor.getValue();
    assertEquals(userId, saved.getUser().getId());
    assertEquals(accommodationId, saved.getAccommodation().getId());
  }

  @Test
  @DisplayName("찜 등록 실패 - 이미 등록된 찜")
  void addWishlistAlreadyExists() {
    // given
    Long userId = 1L;
    Long accommodationId = 100L;

    User user = User.builder().id(userId).email("user@example.com").build();
    Accommodation accommodation = Accommodation.builder().id(accommodationId).name("숙소").build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(accommodationRepository.findById(accommodationId)).thenReturn(Optional.of(accommodation));
    when(wishlistRepository.existsByUserIdAndAccommodationId(userId, accommodationId)).thenReturn(true);

    // when & then
    MeongnyangerangException exception = assertThrows(
        MeongnyangerangException.class,
        () -> wishlistService.addWishlist(userId, accommodationId)
    );

    assertEquals(ErrorCode.ALREADY_WISHLISTED.getDescription(), exception.getErrorCode().getDescription());
    verify(wishlistRepository, never()).save(Mockito.any(Wishlist.class));
  }

  @Test
  @DisplayName("찜 삭제 성공")
  void removeWishlistSuccess() {
    // given
    Long userId = 1L;
    Long accommodationId = 100L;

    User user = User.builder().id(userId).email("user@example.com").build();
    Accommodation accommodation = Accommodation.builder().id(accommodationId).name("숙소").build();

    Wishlist wishlist = Wishlist.builder()
        .id(10L)
        .user(user)
        .accommodation(accommodation)
        .build();

    when(wishlistRepository.findByUserIdAndAccommodationId(userId, accommodationId)).thenReturn(Optional.of(wishlist));

    // when
    wishlistService.removeWishlist(userId, accommodationId);

    // then
    verify(wishlistRepository).delete(wishlist);
  }
}
