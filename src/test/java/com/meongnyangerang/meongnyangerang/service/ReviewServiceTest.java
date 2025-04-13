package com.meongnyangerang.meongnyangerang.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationType;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.host.HostStatus;
import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.review.Review;
import com.meongnyangerang.meongnyangerang.domain.review.ReviewImage;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.UserStatus;
import com.meongnyangerang.meongnyangerang.dto.AccommodationReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.CustomReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.HostReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.LatestReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.MyReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.ReviewContent;
import com.meongnyangerang.meongnyangerang.dto.ReviewImageResponse;
import com.meongnyangerang.meongnyangerang.dto.ReviewRequest;
import com.meongnyangerang.meongnyangerang.dto.UpdateReviewRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.ReservationRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewImageProjection;
import com.meongnyangerang.meongnyangerang.repository.ReviewImageRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
  private AccommodationRepository accommodationRepository;

  @Mock
  private ImageService imageService;

  @InjectMocks
  private ReviewService reviewService;

  private Host host;
  private Accommodation accommodation;
  private List<Review> reviews;
  private List<ReviewImageProjection> reviewImageProjections;

  @Test
  @DisplayName("유저는 예약한 숙소에 대해 리뷰를 작성할 수 있습니다.")
  void createReview_success() {
    // given
    User user = User.builder().id(1L).build();
    Accommodation accommodation = Accommodation.builder().id(1L).totalRating(4.0).build();
    Room room = Room.builder().id(1L)
        .accommodation(accommodation).build();

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

    double previousTotalRating = accommodation.getTotalRating();

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

    assertNotEquals(previousTotalRating, accommodation.getTotalRating());
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
    Room room = Room.builder().id(1L)
        .accommodation(Accommodation.builder().id(1L).totalRating(4.0).build()).build();

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

    Review review1 = Review.builder()
        .id(1L)
        .accommodation(accommodation)
        .user(user)
        .content("content")
        .userRating(3.0)
        .petFriendlyRating(4.0)
        .createdAt(LocalDateTime.now())
        .reportCount(0)
        .build();

    ReviewImage reviewImage1 = ReviewImage.builder().id(1L).review(review1)
        .imageUrl("https://test.com/images/image.jpg").createdAt(LocalDateTime.now()).build();

    Review review2 = Review.builder()
        .id(2L)
        .accommodation(accommodation)
        .user(user)
        .content("content")
        .userRating(3.0)
        .petFriendlyRating(4.0)
        .createdAt(LocalDateTime.now())
        .reportCount(0)
        .build();

    ReviewImage reviewImage2 = ReviewImage.builder().id(2L).review(review2)
        .imageUrl("https://test.com/images/image.jpg").createdAt(LocalDateTime.now()).build();

    List<ReviewImageResponse> list1 = List.of(
        new ReviewImageResponse(reviewImage1.getId(), reviewImage1.getImageUrl())
    );

    MyReviewResponse expectedResponse1 = MyReviewResponse.builder()
        .accommodationName(review1.getAccommodation().getName())
        .reviewImages(list1)
        .totalRating(3.5)
        .content(review1.getContent())
        .createdAt(review1.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        .build();

    when(reviewRepository.findByUserId(user.getId(), 0L, 5 + 1)).thenAnswer(
        invocation -> Stream.of(review1, review2)
            .filter(review -> review.getReportCount() < 20)
            .toList()
    );
    when(reviewImageRepository.findAllByReviewId(review1.getId()))
        .thenReturn(List.of(reviewImage1));

    when(reviewImageRepository.findAllByReviewId(review2.getId()))
        .thenReturn(List.of(reviewImage2));

    // when
    CustomReviewResponse<MyReviewResponse> customResponse = reviewService.getUsersReviews(
        user.getId(), 0L, 5);

    // then
    assertEquals(expectedResponse1.getAccommodationName(),
        customResponse.getContent().get(0).getAccommodationName());
    assertEquals(expectedResponse1.getTotalRating(),
        customResponse.getContent().get(0).getTotalRating());
    assertEquals(expectedResponse1.getReviewImages().get(0).getImageUrl(),
        customResponse.getContent().get(0).getReviewImages().get(0).getImageUrl());
    assertEquals(reviewImage2.getId(),
        customResponse.getContent().get(1).getReviewImages().get(0).getImageId());
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

    Review review1 = Review.builder()
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

    ReviewImage reviewImage1 = ReviewImage.builder().id(1L).review(review1)
        .imageUrl("https://test.com/images/image.jpg").createdAt(LocalDateTime.now()).build();

    Review review2 = Review.builder()
        .id(1L)
        .accommodation(accommodation)
        .reservation(Reservation.builder().id(1L).room(room).build())
        .user(user)
        .content("content")
        .userRating(3.0)
        .petFriendlyRating(4.0)
        .createdAt(LocalDateTime.now())
        .reportCount(20)
        .build();

    ReviewImage reviewImage2 = ReviewImage.builder().id(1L).review(review2)
        .imageUrl("https://test.com/images/image.jpg").createdAt(LocalDateTime.now()).build();

    double totalRating =
        Math.round(((review1.getUserRating() + review1.getPetFriendlyRating()) / 2) * 10) / 10.0;

    AccommodationReviewResponse expectedResponse = AccommodationReviewResponse.builder()
        .roomName(review1.getReservation().getRoom().getName())
        .profileImageUrl(review1.getUser().getProfileImage())
        .reviewImageUrl(reviewImage1.getImageUrl())
        .totalRating(totalRating)
        .content(review1.getContent())
        .createdAt(review1.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        .build();

    when(reviewRepository.findByAccommodationId(accommodation.getId(), 0L, 5 + 1)).thenAnswer(
        invocation -> Stream.of(review1, review2)
            .filter(review -> review.getReportCount() < 20)
            .toList()
    );
    when(reviewImageRepository.findByReviewId(review1.getId())).thenReturn(reviewImage1);
    when(reviewImageRepository.findByReviewId(review2.getId())).thenReturn(reviewImage2);

    // when
    CustomReviewResponse<AccommodationReviewResponse> customResponse = reviewService.getAccommodationReviews(
        accommodation.getId(), 0L, 5);

    // then
    assertEquals(1, customResponse.getContent().size());
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
    Accommodation accommodation = Accommodation.builder().id(1L).totalRating(4.0).build();

    Review review = Review.builder().id(1L).user(user).userRating(3.0).petFriendlyRating(4.0)
        .accommodation(accommodation).build();
    ReviewImage image = ReviewImage.builder().id(1L).review(review).build();

    when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));
    when(reviewImageRepository.findAllByReviewId(review.getId())).thenReturn(List.of(image));

    double previousTotalRating = accommodation.getTotalRating();

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

    assertNotEquals(previousTotalRating, accommodation.getTotalRating());
    assertEquals(0, accommodation.getTotalRating());
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

  @Test
  @DisplayName("유저는 자신의 리뷰를 수정할 수 있다.")
  void updateReview_success() {
    // given
    User user = User.builder().id(1L).build();
    Room room = Room.builder().id(1L)
        .accommodation(Accommodation.builder().id(1L).totalRating(4.0).build()).build();

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
        .createdAt(LocalDateTime.now())
        .build();

    Review review = Review.builder()
        .id(1L)
        .user(user)
        .accommodation(room.getAccommodation())
        .reservation(reservation)
        .userRating(3.0)
        .petFriendlyRating(4.0)
        .content("before")
        .reportCount(0)
        .build();

    String url = "https://test.com/images/image.jpg";
    String newImageUrl = "https://test.com/images/new-image.jpg";

    List<ReviewImage> images = List.of(
        ReviewImage.builder().id(1L).review(review).imageUrl(url).build(),
        ReviewImage.builder().id(2L).review(review).imageUrl(url).build()
    );

    MockMultipartFile mockImageFile = new MockMultipartFile(
        "image", "new-image.jpg", "image/jpeg", new byte[]{1, 2, 3, 4}
    );

    when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));
    when(reviewImageRepository.findAllByReviewId(review.getId())).thenReturn(images);
    when(imageService.storeImage(mockImageFile)).thenReturn(newImageUrl);

    UpdateReviewRequest request = UpdateReviewRequest.builder()
        .content("after")
        .petRating(3.0)
        .userRating(4.0)
        .deletedImageId(List.of(2L))
        .build();

    // when
    reviewService.updateReview(user.getId(), review.getId(), List.of(mockImageFile), request);

    // then
    ArgumentCaptor<ReviewImage> reviewImageCaptor = ArgumentCaptor.forClass(ReviewImage.class);
    verify(reviewImageRepository, times(1)).save(reviewImageCaptor.capture());

    assertEquals(review, reviewImageCaptor.getValue().getReview());
    assertEquals(newImageUrl, reviewImageCaptor.getValue().getImageUrl());
    assertEquals("after", review.getContent());
    assertEquals(3.0, review.getPetFriendlyRating());
    assertEquals(4.0, review.getUserRating());

    ArgumentCaptor<List<ReviewImage>> captor = ArgumentCaptor.forClass(List.class);
    verify(reviewImageRepository, times(1)).deleteAll(captor.capture());
  }

  @Test
  @DisplayName("리뷰가 존재하지 않는 경우, REVIEW_NOT_FOUND 예외가 발생해야 한다.")
  void updateReview_review_not_found() {
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
        .createdAt(LocalDateTime.now())
        .build();

    Review review = Review.builder()
        .id(1L)
        .user(user)
        .accommodation(Accommodation.builder().id(1L).build())
        .reservation(reservation)
        .userRating(3.0)
        .petFriendlyRating(4.0)
        .content("before")
        .reportCount(0)
        .build();

    MockMultipartFile mockImageFile = new MockMultipartFile(
        "image", "new-image.jpg", "image/jpeg", new byte[]{1, 2, 3, 4}
    );

    UpdateReviewRequest request = UpdateReviewRequest.builder()
        .content("after")
        .petRating(3.0)
        .userRating(4.0)
        .deletedImageId(List.of(2L))
        .build();

    // when & then
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class,
        () -> reviewService.updateReview(
            user.getId(), review.getId(), List.of(mockImageFile), request));

    assertEquals(ErrorCode.REVIEW_NOT_FOUND, e.getErrorCode());
  }

  @Test
  @DisplayName("리뷰의 작성자가 아닌 경우, REVIEW_NOT_AUTHORIZED 예외가 발생해야 한다.")
  void updateReview_review_not_authorized() {
    // given
    User user = User.builder().id(2L).build();
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
        .createdAt(LocalDateTime.now())
        .build();

    Review review = Review.builder()
        .id(1L)
        .user(User.builder().id(1L).build())
        .accommodation(Accommodation.builder().id(1L).build())
        .reservation(reservation)
        .userRating(3.0)
        .petFriendlyRating(4.0)
        .content("before")
        .reportCount(0)
        .build();

    MockMultipartFile mockImageFile = new MockMultipartFile(
        "image", "new-image.jpg", "image/jpeg", new byte[]{1, 2, 3, 4}
    );

    UpdateReviewRequest request = UpdateReviewRequest.builder()
        .content("after")
        .petRating(3.0)
        .userRating(4.0)
        .deletedImageId(List.of(2L))
        .build();

    when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));

    // when & then
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class,
        () -> reviewService.updateReview(
            user.getId(), review.getId(), List.of(mockImageFile), request));

    assertEquals(ErrorCode.REVIEW_NOT_AUTHORIZED, e.getErrorCode());
  }

  @Test
  @DisplayName("최대 이미지 개수(3장)을 초과한 경우, MAX_IMAGE_LIMIT_EXCEEDED 예외가 발생해야 한다.")
  void updateReview_max_image_limit_exceeded() {
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
        .createdAt(LocalDateTime.now())
        .build();

    Review review = Review.builder()
        .id(1L)
        .user(user)
        .accommodation(Accommodation.builder().id(1L).build())
        .reservation(reservation)
        .userRating(3.0)
        .petFriendlyRating(4.0)
        .content("before")
        .reportCount(0)
        .build();

    String url = "https://test.com/images/image.jpg";

    List<ReviewImage> images = List.of(
        ReviewImage.builder().id(1L).review(review).imageUrl(url).build(),
        ReviewImage.builder().id(2L).review(review).imageUrl(url).build()
    );

    List<MultipartFile> mockImages = List.of(
        new MockMultipartFile("image1", "new-image1.jpg", "image/jpeg", new byte[]{1, 2, 3, 4}),
        new MockMultipartFile("image2", "new-image2.jpg", "image/jpeg", new byte[]{1, 2, 3, 4})
    );

    UpdateReviewRequest request = UpdateReviewRequest.builder()
        .content("after")
        .petRating(3.0)
        .userRating(4.0)
        .deletedImageId(List.of())
        .build();

    when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));
    when(reviewImageRepository.findAllByReviewId(review.getId())).thenReturn(images);

    // when & then
    MeongnyangerangException e = assertThrows(MeongnyangerangException.class,
        () -> reviewService.updateReview(
            user.getId(), review.getId(), mockImages, request));

    assertEquals(ErrorCode.MAX_IMAGE_LIMIT_EXCEEDED, e.getErrorCode());
  }

  @Test
  @DisplayName("숙소 목록 조회 성공 - 다음 커서 존재")
  void getHostReviews_Success_WithNextCursor() {
    // given
    settingTestReview();
    Long cursorId = null;
    int pageSize = 3;

    List<Review> pagedReviews = reviews.subList(0, pageSize + 1);
    Pageable pageable = PageRequest.of(0, pageSize + 1);

    when(accommodationRepository.findByHostId(host.getId())).thenReturn(Optional.of(accommodation));
    when(reviewRepository.findByAccommodationIdWithCursor(
        accommodation.getId(), cursorId, pageable)).thenReturn(pagedReviews);

    List<Review> actualReviews = pagedReviews.subList(0, pageSize); // 실제 반환될 리뷰
    List<Long> actualReviewIds = actualReviews.stream().map(Review::getId).toList();
    when(reviewImageRepository.findByReviewIds(actualReviewIds))
        .thenReturn(reviewImageProjections.subList(0, 3));

    // when
    HostReviewResponse response = reviewService.getHostReviews(host.getId(), cursorId, pageSize);

    // then
    assertThat(response.content()).hasSize(3);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.cursorId()).isEqualTo(3L);

    // 반환된 리뷰의 ID 검증
    List<Long> returnedIds = response.content().stream()
        .map(ReviewContent::reviewId)
        .toList();
    assertThat(returnedIds).containsExactly(1L, 2L, 3L);

    // 첫 번째 리뷰의 이미지 검증
    ReviewContent firstReview = response.content().get(0);
    assertThat(firstReview.reviewId()).isEqualTo(1L);
    assertThat(firstReview.imageUrls()).contains("image1_1.jpg");

    // 두 번째 리뷰의 이미지 검증
    ReviewContent secondReview = response.content().get(1);
    assertThat(secondReview.reviewId()).isEqualTo(2L);
    assertThat(secondReview.imageUrls()).contains("image2_1.jpg");

    // 세 번째 리뷰의 이미지 검증
    ReviewContent thirdReview = response.content().get(2);
    assertThat(thirdReview.reviewId()).isEqualTo(3L);
    assertThat(thirdReview.imageUrls()).contains("image3_1.jpg");
  }

  @Test
  @DisplayName("숙소 목록 조회 성공 - 다음 커서 없음")
  void getHostReviews_Success_WithoutNextCursor() {
    // given
    settingTestReview();
    Long cursorId = null;
    int pageSize = 5;
    Pageable pageable = PageRequest.of(0, pageSize + 1);

    when(accommodationRepository.findByHostId(host.getId())).thenReturn(Optional.of(accommodation));
    when(reviewRepository.findByAccommodationIdWithCursor(
        accommodation.getId(), cursorId, pageable)).thenReturn(reviews); // ㅁ

    List<Long> actualReviewIds = reviews.stream().map(Review::getId).toList();

    when(reviewImageRepository.findByReviewIds(actualReviewIds)).thenReturn(reviewImageProjections);

    // when
    HostReviewResponse response = reviewService.getHostReviews(host.getId(), cursorId, pageSize);

    // then
    assertThat(response.content()).hasSize(5);
    assertThat(response.hasNext()).isFalse();
    assertThat(response.cursorId()).isEqualTo(null);

    // 모든 리뷰가 포함되었는지 검증
    List<Long> returnedIds = response.content().stream()
        .map(ReviewContent::reviewId)
        .toList();
    assertThat(returnedIds).containsExactly(1L, 2L, 3L, 4L, 5L);

    // 첫 번째 리뷰의 이미지 검증
    ReviewContent firstReview = response.content().get(0);
    assertThat(firstReview.reviewId()).isEqualTo(1L);
    assertThat(firstReview.imageUrls()).contains("image1_1.jpg");

    // 두 번째 리뷰의 이미지 검증
    ReviewContent secondReview = response.content().get(1);
    assertThat(secondReview.reviewId()).isEqualTo(2L);
    assertThat(secondReview.imageUrls()).contains("image2_1.jpg");

    // 세 번째 리뷰의 이미지 검증
    ReviewContent thirdReview = response.content().get(2);
    assertThat(thirdReview.reviewId()).isEqualTo(3L);
    assertThat(thirdReview.imageUrls()).contains("image3_1.jpg");

    // 네 번째 리뷰의 이미지 검증
    ReviewContent fourthReview = response.content().get(3);
    assertThat(fourthReview.reviewId()).isEqualTo(4L);
    assertThat(fourthReview.imageUrls()).contains("image4_1.jpg");

    // 다섯 번째 리뷰의 이미지 검증
    ReviewContent fifthReview_1 = response.content().get(4);
    assertThat(fifthReview_1.reviewId()).isEqualTo(5L);
    assertThat(fifthReview_1.imageUrls()).contains("image5_1.jpg");

    // 다섯2 번째 리뷰의 이미지 검증
    ReviewContent fifthReview_2 = response.content().get(4);
    assertThat(fifthReview_2.reviewId()).isEqualTo(5L);
    assertThat(fifthReview_2.imageUrls()).contains("image5_2.jpg");
  }

  @Test
  @DisplayName("숙소 목록 조회 실패 - 숙소 존재하지 않음")
  void getHostReviews_AccommodationNotFound_ThrowsException() {
    // given
    Long cursorId = null;
    int pageSize = 5;
    Long nonExistentHostId = 999L;

    when(accommodationRepository.findByHostId(nonExistentHostId)).thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(() -> reviewService.getHostReviews(nonExistentHostId, cursorId, pageSize))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.ACCOMMODATION_NOT_FOUND);
  }

  @Test
  @DisplayName("로그인한 유저는 최신 리뷰를 조회할 수 있다. (10개)")
  void getLatestReviews_success() {
    // given
    List<Review> dummyReviews = createDummyReviews(10);

    when(reviewRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(dummyReviews);

    for (Review review : dummyReviews) {
      ReviewImage reviewImage = ReviewImage.builder()
          .id(1L)
          .review(review)
          .imageUrl("http://example.com/review" + review.getId() + ".jpg")
          .createdAt(LocalDateTime.now())
          .build();

      when(reviewImageRepository.findFirstByReviewIdOrderByIdAsc(review.getId()))
          .thenReturn(reviewImage);
    }

    // when
    List<LatestReviewResponse> result = reviewService.getLatestReviews();

    // then
    assertEquals(10, result.size());
    assertEquals("Review content 0", result.get(0).getContent());
    assertEquals("http://example.com/review1.jpg", result.get(0).getImageUrl());
  }

  public List<Review> createDummyReviews(int count) {
    List<Review> reviews = new ArrayList<>();

    for (int i = 0; i < count; i++) {
      User user = User.builder()
          .id((long) i + 1)
          .email("user" + i + "@test.com")
          .nickname("user" + i)
          .password("pass" + i)
          .status(UserStatus.ACTIVE)
          .role(Role.ROLE_USER)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .build();

      Accommodation accommodation = Accommodation.builder()
          .id((long) i + 1)
          .name("Test Accommodation " + i)
          .address("Seoul")
          .latitude(37.0)
          .longitude(127.0)
          .type(AccommodationType.PENSION)
          .thumbnailUrl("http://example.com/image" + i + ".jpg")
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .host(null)
          .build();

      Reservation reservation = Reservation.builder()
          .id((long) i + 1)
          .user(user)
          .room(Room.builder()
              .id((long) i + 1)
              .name("Room " + i)
              .accommodation(accommodation)
              .standardPeopleCount(2)
              .maxPeopleCount(4)
              .standardPetCount(1)
              .maxPetCount(2)
              .imageUrl("http://example.com/room" + i + ".jpg")
              .price(10000L)
              .extraFee(0L)
              .checkInTime(LocalTime.of(15, 0))
              .checkOutTime(LocalTime.of(11, 0))
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .build())
          .accommodationName(accommodation.getName())
          .checkInDate(LocalDate.now())
          .checkOutDate(LocalDate.now().plusDays(1))
          .peopleCount(2)
          .petCount(1)
          .reserverName("Reserver " + i)
          .reserverPhoneNumber("0101234567" + i)
          .hasVehicle(true)
          .totalPrice(120000L)
          .status(ReservationStatus.COMPLETED)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .build();

      Review review = Review.builder()
          .id((long) i + 1)
          .user(user)
          .accommodation(accommodation)
          .reservation(reservation)
          .userRating(4.0 + (i % 2))
          .petFriendlyRating(4.5)
          .content("Review content " + i)
          .reportCount(0)
          .createdAt(LocalDateTime.now().minusDays(i))
          .updatedAt(LocalDateTime.now().minusDays(i))
          .build();

      reviews.add(review);
    }

    return reviews;
  }

  private void settingTestReview() {
    Long hostId = 1L;
    Long accommodationId = 10L;

    host = Host.builder()
        .id(hostId)
        .build();

    // 숙소 설정
    accommodation = Accommodation.builder()
        .id(accommodationId)
        .host(host)
        .name("테스트 숙소")
        .build();

    reviews = new ArrayList<>();
    for (long i = 1; i <= 5; i++) {
      Review review = createTestReview(i, "리뷰 내용 " + i);
      reviews.add(review);
    }

    // 테스트 리뷰 이미지 프로젝션 생성
    reviewImageProjections = new ArrayList<>();
    reviewImageProjections.add(createReviewImageProjection(1L, "image1_1.jpg"));
    reviewImageProjections.add(createReviewImageProjection(2L, "image2_1.jpg"));
    reviewImageProjections.add(createReviewImageProjection(3L, "image3_1.jpg"));
    reviewImageProjections.add(createReviewImageProjection(4L, "image4_1.jpg"));
    reviewImageProjections.add(createReviewImageProjection(5L, "image5_1.jpg"));
    reviewImageProjections.add(createReviewImageProjection(5L, "image5_2.jpg"));
  }

  private Review createTestReview(Long id, String content) {
    User user = User.builder()
        .id(100L + id)
        .build();

    Room room = Room.builder()
        .id(200L + id)
        .name("객실 " + id)
        .accommodation(accommodation)
        .build();

    Reservation reservation = Reservation.builder()
        .id(300L + id)
        .room(room)
        .build();

    return Review.builder()
        .id(id)
        .user(user)
        .reservation(reservation)
        .userRating(4.0)
        .petFriendlyRating(4.5)
        .content(content)
        .createdAt(LocalDateTime.now().minusDays(id))
        .build();
  }

  private ReviewImageProjection createReviewImageProjection(Long reviewId, String imageUrl) {
    return new ReviewImageProjection() {
      @Override
      public Long getReviewId() {
        return reviewId;
      }

      @Override
      public String getImageUrl() {
        return imageUrl;
      }
    };
  }
}