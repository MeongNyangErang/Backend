package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.review.Review;
import com.meongnyangerang.meongnyangerang.domain.review.ReviewImage;
import com.meongnyangerang.meongnyangerang.dto.ReviewRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.ReservationRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewImageRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.time.LocalDateTime;
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
}
