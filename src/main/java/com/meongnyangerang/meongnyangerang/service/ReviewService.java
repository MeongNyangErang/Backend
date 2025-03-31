package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.review.Review;
import com.meongnyangerang.meongnyangerang.domain.review.ReviewImage;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ImageService imageService;
  private final ReviewRepository reviewRepository;
  private final ReservationRepository reservationRepository;
  private final ReviewImageRepository reviewImageRepository;

  @Transactional
  public void createReview(Long userId, ReviewRequest reviewRequest, List<MultipartFile> images) {
    // 예약 정보 가져오기
    Reservation reservation = reservationRepository.findById(reviewRequest.getReservationId())
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.RESERVATION_NOT_FOUND));

    // 리뷰 작성 가능 여부 검증
    validateReviewCreation(userId, reservation);

    Review review = reviewRequest.toEntity(reservation.getUser(),
        reservation.getRoom().getAccommodation(), reservation);

    // 이미지 등록 (있는 경우에만)
    if (images != null) {
      validateImageSize(images);
      addImages(images, review);
    }

    reviewRepository.save(review);
  }

  private void validateImageSize(List<MultipartFile> images) {
    if (images.size() > 3) {
      throw new MeongnyangerangException(ErrorCode.MAX_IMAGE_LIMIT_EXCEEDED);
    }
  }

  private void addImages(List<MultipartFile> images, Review review) {
    images.stream()
        .filter(image -> !image.isEmpty())
        .map(imageService::storeImage)
        .map(url -> ReviewImage.builder().review(review).imageUrl(url).build())
        .forEach(reviewImageRepository::save);
  }

  private void validateReviewCreation(Long userId, Reservation reservation) {
    // 이미 작성된 리뷰인지 확인
    if (reviewRepository.existsByUserIdAndReservationId(userId, reservation.getId())) {
      throw new MeongnyangerangException(ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    // 예약한 사용자와 로그인한 사용자가 같은지 확인
    if (!Objects.equals(reservation.getUser().getId(), userId)) {
      throw new MeongnyangerangException(ErrorCode.INVALID_AUTHORIZED);
    }

    // 예약 생성 후 7일이 지나거나, 예약 상태가 RESERVED 가 아닌 경우 예외 발생
    if (reservation.getCreatedAt().plusDays(7).isBefore(LocalDateTime.now()) ||
        reservation.getStatus() != ReservationStatus.RESERVED) {
      throw new MeongnyangerangException(ErrorCode.REVIEW_CREATION_NOT_ALLOWED);
    }
  }

  public CustomReviewResponse<MyReviewResponse> getUsersReviews(Long userId, Long cursorId,
      Integer size) {
    // 해당 유저의 리뷰 내역만 조회
    List<Review> reviews = reviewRepository.findByUserId(userId, cursorId, size + 1);

    List<MyReviewResponse> content = reviews.stream()
        .limit(size)
        .map(this::mapToMyReviewResponse)
        .toList();

    boolean hasNext = reviews.size() > size;
    Long cursor = hasNext ? reviews.get(size).getId() : null;

    return new CustomReviewResponse<>(content, cursor, hasNext);
  }

  private MyReviewResponse mapToMyReviewResponse(Review review) {
    ReviewImage reviewImage = reviewImageRepository.findByReviewId(review.getId());

    // 소숫점 한자리까지만 필요
    double totalRating =
        Math.round(((review.getUserRating() + review.getPetFriendlyRating()) / 2) * 10) / 10.0;

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    return MyReviewResponse.builder()
        .accommodationName(review.getAccommodation().getName())
        .reviewImageUrl(reviewImage.getImageUrl())
        .totalRating(totalRating)
        .content(review.getContent())
        .createdAt(review.getCreatedAt().format(dateFormatter))
        .build();
  }

  public CustomReviewResponse<AccommodationReviewResponse> getAccommodationReviews(
      Long accommodationId,
      Long cursorId, Integer size) {
    // 해당 숙소의 리뷰 내역만 조회
    List<Review> reviews = reviewRepository.findByAccommodationId(accommodationId, cursorId,
        size + 1);

    List<AccommodationReviewResponse> content = reviews.stream()
        .limit(size)
        .map(this::mapToAccommodationReviewResponse)
        .toList();

    boolean hasNext = reviews.size() > size;
    Long cursor = hasNext ? reviews.get(size).getId() : null;

    return new CustomReviewResponse<>(content, cursor, hasNext);
  }

  private AccommodationReviewResponse mapToAccommodationReviewResponse(Review review) {
    ReviewImage reviewImage = reviewImageRepository.findByReviewId(review.getId());

    // 소숫점 한자리까지만 필요
    double totalRating =
        Math.round(((review.getUserRating() + review.getPetFriendlyRating()) / 2) * 10) / 10.0;

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    return AccommodationReviewResponse.builder()
        .roomName(review.getReservation().getRoom().getName())
        .profileImageUrl(review.getUser().getProfileImage())
        .reviewImageUrl(reviewImage.getImageUrl())
        .totalRating(totalRating)
        .content(review.getContent())
        .createdAt(review.getCreatedAt().format(dateFormatter))
        .build();
  }

  public void deleteReview(Long reviewId, Long userId) {
    // 리뷰 조회
    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.REVIEW_NOT_FOUND));

    // 요청한 사용자가 작성자인지 확인
    if (!review.getUser().getId().equals(userId)) {
      throw new MeongnyangerangException(ErrorCode.REVIEW_NOT_AUTHORIZED);
    }

    // 리뷰 & 리뷰 이미지 삭제
    reviewRepository.delete(review);
    reviewImageRepository.deleteAll(reviewImageRepository.findAllByReviewId(reviewId));
  }
}
