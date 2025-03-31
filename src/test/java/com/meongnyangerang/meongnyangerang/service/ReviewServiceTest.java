package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.review.Review;
import com.meongnyangerang.meongnyangerang.domain.review.ReviewImage;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.AccommodationReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.CustomReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.MyReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.ReviewRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.ReservationRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewImageRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @Mock
  private ReservationRepository reservationRepository;

  @Mock
  private ReviewRepository reviewRepository;

  @Mock
  private ReviewImageRepository reviewImageRepository;

  @Mock
  private ImageService imageService;

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

    List<MultipartFile> images = List.of(
        new MockMultipartFile("image", "image.jpg", "image/jpeg", new byte[]{1, 2, 3, 4})
    );

    String url = "https://test.com/images/image.jpg";

    when(reservationRepository.findById(request.getReservationId())).thenReturn(
        Optional.of(reservation));
    when(reviewRepository.existsByUserIdAndReservationId(user.getId(),
        request.getReservationId())).thenReturn(false);
    when(imageService.storeImage(images.get(0))).thenReturn(url);

    // when
    reviewService.createReview(user.getId(), request, images);

    // then
    ArgumentCaptor<ReviewImage> imageCaptor = ArgumentCaptor.forClass(ReviewImage.class);
    verify(reviewImageRepository, times(1)).save(imageCaptor.capture());

    ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
    verify(reviewRepository, times(1)).save(reviewCaptor.capture());

    verify(reservationRepository, times(1)).findById(request.getReservationId());
    verify(reviewRepository, times(1)).existsByUserIdAndReservationId(user.getId(),
        request.getReservationId());

    ReviewImage capturedReviewImage = imageCaptor.getValue();
    assertEquals(url, capturedReviewImage.getImageUrl());
    assertEquals(user, capturedReviewImage.getReview().getUser());

    Review capturedReview = reviewCaptor.getValue();
    assertEquals(3.0, capturedReview.getUserRating());
    assertEquals(4.0, capturedReview.getPetFriendlyRating());
    assertEquals("반려동물이 즐거워해요", capturedReview.getContent());
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

    List<MultipartFile> images = List.of(
        new MockMultipartFile("image", "image.jpg", "image/jpeg", new byte[]{1, 2, 3, 4})
    );

    when(reservationRepository.findById(request.getReservationId())).thenReturn(
        Optional.empty());

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reviewService.createReview(user.getId(), request, images);
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

    List<MultipartFile> images = List.of(
        new MockMultipartFile("image", "image.jpg", "image/jpeg", new byte[]{1, 2, 3, 4})
    );

    when(reservationRepository.findById(request.getReservationId())).thenReturn(
        Optional.of(reservation));
    when(reviewRepository.existsByUserIdAndReservationId(user.getId(), request.getReservationId()))
        .thenReturn(true);

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reviewService.createReview(user.getId(), request, images);
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

    List<MultipartFile> images = List.of(
        new MockMultipartFile("image", "image.jpg", "image/jpeg", new byte[]{1, 2, 3, 4})
    );

    when(reservationRepository.findById(request.getReservationId())).thenReturn(
        Optional.of(reservation));
    when(reviewRepository.existsByUserIdAndReservationId(100L, request.getReservationId()))
        .thenReturn(false);

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reviewService.createReview(100L, request, images);
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

    List<MultipartFile> images = List.of(
        new MockMultipartFile("image", "image.jpg", "image/jpeg", new byte[]{1, 2, 3, 4})
    );

    when(reservationRepository.findById(request.getReservationId())).thenReturn(
        Optional.of(reservation));
    when(reviewRepository.existsByUserIdAndReservationId(user.getId(), request.getReservationId()))
        .thenReturn(false);

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reviewService.createReview(user.getId(), request, images);
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

    List<MultipartFile> images = List.of(
        new MockMultipartFile("image", "image.jpg", "image/jpeg", new byte[]{1, 2, 3, 4})
    );

    when(reservationRepository.findById(request.getReservationId())).thenReturn(
        Optional.of(reservation));
    when(reviewRepository.existsByUserIdAndReservationId(user.getId(), request.getReservationId()))
        .thenReturn(false);

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reviewService.createReview(user.getId(), request, images);
    });

    // then
    assertEquals(ErrorCode.REVIEW_CREATION_NOT_ALLOWED, e.getErrorCode());
  }

  @Test
  @DisplayName("이미지 개수가 3개를 초과한 경우, MAX_IMAGE_LIMIT_EXCEEDED 예외가 발생해야 한다.")
  void createReview_review_max_image_limit_exceeded() {
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

    List<MultipartFile> images = List.of(
        new MockMultipartFile("image1", "image1.jpg", "image/jpeg", new byte[]{1, 2, 3, 4}),
        new MockMultipartFile("image2", "image2.jpg", "image/jpeg", new byte[]{1, 2, 3, 4}),
        new MockMultipartFile("image3", "image3.jpg", "image/jpeg", new byte[]{1, 2, 3, 4}),
        new MockMultipartFile("image4", "image4.jpg", "image/jpeg", new byte[]{1, 2, 3, 4})
    );

    when(reservationRepository.findById(request.getReservationId())).thenReturn(
        Optional.of(reservation));
    when(reviewRepository.existsByUserIdAndReservationId(user.getId(), request.getReservationId()))
        .thenReturn(false);

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class, () -> {
      reviewService.createReview(user.getId(), request, images);
    });

    // then
    assertEquals(ErrorCode.MAX_IMAGE_LIMIT_EXCEEDED, e.getErrorCode());
  }

  @Test
  @DisplayName("유저는 자신의 리뷰만 조회가 가능하다.")
  void getUserReviews_success() {
    // given
    User user = User.builder().id(1L).build();

    Accommodation accommodation = Accommodation.builder().id(1L).build();

    Review review = Review.builder()
        .id(1L)
        .accommodation(accommodation)
        .user(user)
        .content("content")
        .userRating(3.0)
        .petFriendlyRating(4.0)
        .createdAt(LocalDateTime.now())
        .reportCount(0)
        .build();

    ReviewImage reviewImage = ReviewImage.builder().id(1L).review(review)
        .imageUrl("https://test.com/images/image.jpg").createdAt(LocalDateTime.now()).build();

    MyReviewResponse expectedResponse = MyReviewResponse.builder()
        .accommodationName(review.getAccommodation().getName())
        .reviewImageUrl(reviewImage.getImageUrl())
        .totalRating(3.5)
        .content(review.getContent())
        .createdAt(review.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        .build();

    when(reviewRepository.findByUserId(user.getId(), 0L, 5 + 1)).thenReturn(List.of(review));
    when(reviewImageRepository.findByReviewId(review.getId())).thenReturn(reviewImage);

    // when
    CustomReviewResponse<MyReviewResponse> customResponse = reviewService.getUsersReviews(
        user.getId(), 0L, 5);

    // then
    assertEquals(expectedResponse.getAccommodationName(),
        customResponse.getContent().get(0).getAccommodationName());
    assertEquals(expectedResponse.getTotalRating(),
        customResponse.getContent().get(0).getTotalRating());
    assertNull(customResponse.getCursor());
    assertFalse(customResponse.isHasNext());
  }

  @Test
  @DisplayName("비로그인 사용자는 숙소 리뷰 조회가 가능하다.")
  void getAccommodationReviews_success() {
    // given
    User user = User.builder().id(1L).profileImage("https://test.com/images/image.jpg").build();

    Accommodation accommodation = Accommodation.builder().id(1L).build();
    Room room = Room.builder().id(1L).name("test").build();

    Review review = Review.builder()
        .id(1L)
        .accommodation(accommodation)
        .reservation(Reservation.builder().id(1L).room(room).build())
        .user(user)
        .content("content")
        .userRating(3.0)
        .petFriendlyRating(4.0)
        .createdAt(LocalDateTime.now())
        .reportCount(0)
        .build();

    ReviewImage reviewImage = ReviewImage.builder().id(1L).review(review)
        .imageUrl("https://test.com/images/image.jpg").createdAt(LocalDateTime.now()).build();

    double totalRating =
        Math.round(((review.getUserRating() + review.getPetFriendlyRating()) / 2) * 10) / 10.0;

    AccommodationReviewResponse expectedResponse = AccommodationReviewResponse.builder()
        .roomName(review.getReservation().getRoom().getName())
        .profileImageUrl(review.getUser().getProfileImage())
        .reviewImageUrl(reviewImage.getImageUrl())
        .totalRating(totalRating)
        .content(review.getContent())
        .createdAt(review.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        .build();

    when(reviewRepository.findByAccommodationId(accommodation.getId(), 0L, 5 + 1)).thenReturn(
        List.of(review));
    when(reviewImageRepository.findByReviewId(review.getId())).thenReturn(reviewImage);

    // when
    CustomReviewResponse<AccommodationReviewResponse> customResponse = reviewService.getAccommodationReviews(
        accommodation.getId(), 0L, 5);

    // then
    assertEquals(expectedResponse.getNickname(),
        customResponse.getContent().get(0).getNickname());
    assertEquals(expectedResponse.getTotalRating(),
        customResponse.getContent().get(0).getTotalRating());
    assertNull(customResponse.getCursor());
    assertFalse(customResponse.isHasNext());
  }

  @Test
  @DisplayName("유저는 자신의 리뷰를 삭제할 수 있다.")
  void deleteReview_success() {
    // when
    User user = User.builder().id(1L).build();
    Accommodation accommodation = Accommodation.builder().id(1L).build();

    Review review = Review.builder().id(1L).user(user).accommodation(accommodation).build();
    ReviewImage image = ReviewImage.builder().id(1L).review(review).build();

    when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));
    when(reviewImageRepository.findAllByReviewId(review.getId())).thenReturn(List.of(image));

    // when
    reviewService.deleteReview(review.getId(), user.getId());

    // then
    ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
    verify(reviewRepository, times(1)).delete(reviewCaptor.capture());
    assertEquals(review.getId(), reviewCaptor.getValue().getId());

    ArgumentCaptor<List<ReviewImage>> reviewImagesCaptor = ArgumentCaptor.forClass(List.class);
    verify(reviewImageRepository, times(1)).deleteAll(reviewImagesCaptor.capture());
    assertEquals(1, reviewImagesCaptor.getValue().size());
    assertEquals(image.getId(), reviewImagesCaptor.getValue().get(0).getId());
  }

  @Test
  @DisplayName("리뷰가 존재하지 않는 경우, REVIEW_NOT_FOUND 예외가 발생해야 한다.")
  void deleteReview_review_not_found() {
    // when
    User user = User.builder().id(1L).build();
    Accommodation accommodation = Accommodation.builder().id(1L).build();

    Review review = Review.builder().id(1L).user(user).accommodation(accommodation).build();

    when(reviewRepository.findById(review.getId())).thenReturn(Optional.empty());

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class,
        () -> reviewService.deleteReview(review.getId(), user.getId()));

    // then
    assertEquals(ErrorCode.REVIEW_NOT_FOUND, e.getErrorCode());
  }

  @Test
  @DisplayName("삭제 요청한 사용자와 리뷰 작성자가 다를 경우, REVIEW_NOT_AUTHORIZED 예외가 발생해야 한다.")
  void deleteReview_review_not_authorized() {
    // when
    User user = User.builder().id(1L).build();
    Accommodation accommodation = Accommodation.builder().id(1L).build();

    Review review = Review.builder().id(1L).user(User.builder().id(2L).build())
        .accommodation(accommodation).build();

    when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));

    // when
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class,
        () -> reviewService.deleteReview(review.getId(), user.getId()));

    // then
    assertEquals(ErrorCode.REVIEW_NOT_AUTHORIZED, e.getErrorCode());
  }
}