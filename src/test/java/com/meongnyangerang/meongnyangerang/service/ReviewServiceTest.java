package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.ReviewRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.ReservationRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @Mock
  private ReservationRepository reservationRepository;

  @Mock
  private ReviewRepository reviewRepository;

  @InjectMocks
  private ReviewService reviewService;

  @Test
  @DisplayName("유저는 예약한 숙소에 대해 리뷰를 작성할 수 있습니다.")
  void createReview_success() {
    // given
    User user = User.builder().id(1L).build();
    Room room = Room.builder().id(1L).build();

    Reservation reservation = Reservation.builder()
        .id(1L)
        .status(ReservationStatus.RESERVED)
        .user(user)
        .room(room)
        .checkInDate(LocalDate.of(2025, 3, 30))
        .checkOutDate(LocalDate.of(2025, 4, 1))
        .peopleCount(2)
        .petCount(1)
        .totalPrice(30000L)
        .createdAt(LocalDateTime.now())
        .build();

    ReviewRequest request = ReviewRequest.builder()
        .reservationId(1L)
        .userRating(3.0)
        .petFriendlyRating(4.0)
        .content("반려동물이 즐거워해요")
        .build();

    when(reservationRepository.findById(request.getReservationId())).thenReturn(
        Optional.of(reservation));
    when(reviewRepository.existsByUserIdAndReservationId(user.getId(),
        request.getReservationId())).thenReturn(false);

    // when
    reviewService.createReview(user.getId(), request);

    // then
    verify(reservationRepository, times(1)).findById(request.getReservationId());
    verify(reviewRepository, times(1)).existsByUserIdAndReservationId(user.getId(),
        request.getReservationId());
  }

  @Test
  @DisplayName("예약 정보가 없는 경우, RESERVATION_NOT_FOUND 예외가 발생해야 한다.")
  void createReview_reservation_not_found() {
    // given
    User user = User.builder().id(1L).build();
    Room room = Room.builder().id(1L).build();

    Reservation reservation = Reservation.builder()
        .id(1L)
        .status(ReservationStatus.RESERVED)
        .user(user)
        .room(room)
        .checkInDate(LocalDate.of(2025, 3, 30))
        .checkOutDate(LocalDate.of(2025, 4, 1))
        .peopleCount(2)
        .petCount(1)
        .totalPrice(30000L)
        .createdAt(LocalDateTime.now())
        .build();

    ReviewRequest request = ReviewRequest.builder()
        .reservationId(1L)
        .userRating(3.0)
        .petFriendlyRating(4.0)
        .content("반려동물이 즐거워해요")
        .build();

    when(reservationRepository.findById(request.getReservationId())).thenReturn(
        Optional.empty());

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reviewService.createReview(user.getId(), request);
    });

    // then
    assertEquals(ErrorCode.RESERVATION_NOT_FOUND, e.getErrorCode());
  }

  @Test
  @DisplayName("이미 작성된 리뷰인 경우, REVIEW_ALREADY_EXISTS 예외가 발생해야 한다.")
  void createReview_review_already_exists() {
    // given
    User user = User.builder().id(1L).build();
    Room room = Room.builder().id(1L).build();

    Reservation reservation = Reservation.builder()
        .id(1L)
        .status(ReservationStatus.RESERVED)
        .user(user)
        .room(room)
        .checkInDate(LocalDate.of(2025, 3, 30))
        .checkOutDate(LocalDate.of(2025, 4, 1))
        .peopleCount(2)
        .petCount(1)
        .totalPrice(30000L)
        .createdAt(LocalDateTime.now())
        .build();

    ReviewRequest request = ReviewRequest.builder()
        .reservationId(1L)
        .userRating(3.0)
        .petFriendlyRating(4.0)
        .content("반려동물이 즐거워해요")
        .build();

    when(reservationRepository.findById(request.getReservationId())).thenReturn(
        Optional.of(reservation));
    when(reviewRepository.existsByUserIdAndReservationId(user.getId(), request.getReservationId()))
        .thenReturn(true);

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reviewService.createReview(user.getId(), request);
    });

    // then
    assertEquals(ErrorCode.REVIEW_ALREADY_EXISTS, e.getErrorCode());
  }

  @Test
  @DisplayName("예약한 사용자와 로그인한 사용자가 다른 경우, INVALID_AUTHORIZED 예외가 발생해야 한다.")
  void createReview_invalid_authorized() {
    // given
    User user = User.builder().id(1L).build();
    Room room = Room.builder().id(1L).build();

    Reservation reservation = Reservation.builder()
        .id(1L)
        .status(ReservationStatus.RESERVED)
        .user(user)
        .room(room)
        .checkInDate(LocalDate.of(2025, 3, 30))
        .checkOutDate(LocalDate.of(2025, 4, 1))
        .peopleCount(2)
        .petCount(1)
        .totalPrice(30000L)
        .createdAt(LocalDateTime.now())
        .build();

    ReviewRequest request = ReviewRequest.builder()
        .reservationId(1L)
        .userRating(3.0)
        .petFriendlyRating(4.0)
        .content("반려동물이 즐거워해요")
        .build();

    when(reservationRepository.findById(request.getReservationId())).thenReturn(
        Optional.of(reservation));
    when(reviewRepository.existsByUserIdAndReservationId(100L, request.getReservationId()))
        .thenReturn(false);

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reviewService.createReview(100L, request);
    });

    // then
    assertEquals(ErrorCode.INVALID_AUTHORIZED, e.getErrorCode());
  }

  @Test
  @DisplayName("예약 생성 후 7일이 지난 경우, REVIEW_CREATION_NOT_ALLOWED 예외가 발생해야 한다.")
  void createReview_review_creation_not_allowed() {
    // given
    User user = User.builder().id(1L).build();
    Room room = Room.builder().id(1L).build();

    Reservation reservation = Reservation.builder()
        .id(1L)
        .status(ReservationStatus.RESERVED)
        .user(user)
        .room(room)
        .checkInDate(LocalDate.of(2025, 3, 30))
        .checkOutDate(LocalDate.of(2025, 4, 1))
        .peopleCount(2)
        .petCount(1)
        .totalPrice(30000L)
        .createdAt(LocalDateTime.now().minusDays(8))
        .build();

    ReviewRequest request = ReviewRequest.builder()
        .reservationId(1L)
        .userRating(3.0)
        .petFriendlyRating(4.0)
        .content("반려동물이 즐거워해요")
        .build();

    when(reservationRepository.findById(request.getReservationId())).thenReturn(
        Optional.of(reservation));
    when(reviewRepository.existsByUserIdAndReservationId(user.getId(), request.getReservationId()))
        .thenReturn(false);

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reviewService.createReview(user.getId(), request);
    });

    // then
    assertEquals(ErrorCode.REVIEW_CREATION_NOT_ALLOWED, e.getErrorCode());
  }

  @Test
  @DisplayName("예약 상태가 RESERVED 가 아닌 경우, REVIEW_CREATION_NOT_ALLOWED 예외가 발생해야 한다.")
  void createReview_review_creation_not_allowed2() {
    // given
    User user = User.builder().id(1L).build();
    Room room = Room.builder().id(1L).build();

    Reservation reservation = Reservation.builder()
        .id(1L)
        .status(ReservationStatus.COMPLETED)
        .user(user)
        .room(room)
        .checkInDate(LocalDate.of(2025, 3, 30))
        .checkOutDate(LocalDate.of(2025, 4, 1))
        .peopleCount(2)
        .petCount(1)
        .totalPrice(30000L)
        .createdAt(LocalDateTime.now().minusDays(8))
        .build();

    ReviewRequest request = ReviewRequest.builder()
        .reservationId(1L)
        .userRating(3.0)
        .petFriendlyRating(4.0)
        .content("반려동물이 즐거워해요")
        .build();

    when(reservationRepository.findById(request.getReservationId())).thenReturn(
        Optional.of(reservation));
    when(reviewRepository.existsByUserIdAndReservationId(user.getId(), request.getReservationId()))
        .thenReturn(false);

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reviewService.createReview(user.getId(), request);
    });

    // then
    assertEquals(ErrorCode.REVIEW_CREATION_NOT_ALLOWED, e.getErrorCode());
  }
}