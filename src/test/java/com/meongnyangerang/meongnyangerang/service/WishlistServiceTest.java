package com.meongnyangerang.meongnyangerang.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.Wishlist;
import com.meongnyangerang.meongnyangerang.dto.CustomWishlistResponse;
import com.meongnyangerang.meongnyangerang.dto.WishlistResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.repository.WishlistRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import java.util.List;
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

  @Test
  @DisplayName("찜 삭제 실패 - 존재하지 않는 찜")
  void removeWishlistNotFound() {
    // given
    Long userId = 1L;
    Long accommodationId = 100L;

    when(wishlistRepository.findByUserIdAndAccommodationId(userId, accommodationId)).thenReturn(Optional.empty());

    // when & then
    MeongnyangerangException exception = assertThrows(
        MeongnyangerangException.class,
        () -> wishlistService.removeWishlist(userId, accommodationId)
    );

    assertEquals(ErrorCode.NOT_EXIST_WISHLIST.getDescription(), exception.getErrorCode().getDescription());
    verify(wishlistRepository, never()).delete(Mockito.any(Wishlist.class));
  }

  @Test
  @DisplayName("찜 목록 조회 - hasNext 있음")
  void getUserWishlists_HasNext() {
    // given
    Long userId = 1L;
    Long cursorId = 0L;
    int size = 2;

    Accommodation acc1 = Accommodation.builder()
        .id(100L)
        .name("숙소1")
        .thumbnailUrl("thumb1.jpg")
        .address("서울시 강남구")
        .build();

    Accommodation acc2 = Accommodation.builder()
        .id(101L)
        .name("숙소2")
        .thumbnailUrl("thumb2.jpg")
        .address("서울시 종로구")
        .build();

    Accommodation acc3 = Accommodation.builder()
        .id(102L)
        .name("숙소3")
        .thumbnailUrl("thumb3.jpg")
        .address("서울시 마포구")
        .build();

    Wishlist w1 = Wishlist.builder().id(1L).accommodation(acc1).build();
    Wishlist w2 = Wishlist.builder().id(2L).accommodation(acc2).build();
    Wishlist w3 = Wishlist.builder().id(3L).accommodation(acc3).build();

    List<Wishlist> wishlistMock = List.of(w1, w2, w3); // size+1 → hasNext true

    when(wishlistRepository.findByUserId(userId, cursorId, size + 1)).thenReturn(wishlistMock);

    // when
    CustomWishlistResponse<WishlistResponse> result = wishlistService.getUserWishlists(userId, cursorId, size);

    // then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.isHasNext()).isTrue();
    assertThat(result.getNextCursor()).isEqualTo(3L);

    WishlistResponse first = result.getContent().get(0);
    assertThat(first.getAccommodationName()).isEqualTo("숙소1");
    assertThat(first.getAddress()).isEqualTo("서울시 강남구");
  }

  @Test
  @DisplayName("찜 목록 조회 - hasNext 없음")
  void getUserWishlists_HasNextFalse() {
    // given
    Long userId = 1L;
    Long cursorId = 0L;
    int size = 2;

    Accommodation acc1 = Accommodation.builder()
        .id(100L)
        .name("숙소1")
        .thumbnailUrl("thumb1.jpg")
        .address("서울시 강남구")
        .build();

    Accommodation acc2 = Accommodation.builder()
        .id(101L)
        .name("숙소2")
        .thumbnailUrl("thumb2.jpg")
        .address("서울시 종로구")
        .build();

    Wishlist w1 = Wishlist.builder().id(1L).accommodation(acc1).build();
    Wishlist w2 = Wishlist.builder().id(2L).accommodation(acc2).build();

    List<Wishlist> wishlistMock = List.of(w1, w2); // size == list.size → hasNext false

    when(wishlistRepository.findByUserId(userId, cursorId, size + 1)).thenReturn(wishlistMock);

    // when
    CustomWishlistResponse<WishlistResponse> result = wishlistService.getUserWishlists(userId, cursorId, size);

    // then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.isHasNext()).isFalse();
    assertThat(result.getNextCursor()).isNull();
  }
}
