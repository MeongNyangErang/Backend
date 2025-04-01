package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.Wishlist;
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
}
