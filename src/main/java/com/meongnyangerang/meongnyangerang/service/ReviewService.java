package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.review.Review;
import com.meongnyangerang.meongnyangerang.domain.review.ReviewImage;
import com.meongnyangerang.meongnyangerang.dto.AccommodationReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.CustomReviewResponse;
import com.meongnyangerang.meongnyangerang.dto.HostReviewResponse;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
  private final AccommodationRepository accommodationRepository;

  @Transactional
  public void createReview(Long userId, ReviewRequest reviewRequest, List<MultipartFile> images) {
    // 예약 정보 가져오기
    Reservation reservation = reservationRepository.findById(reviewRequest.getReservationId())
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.RESERVATION_NOT_FOUND));

    // 리뷰 작성 가능 여부 검증
    validateReviewCreation(userId, reservation);

    Review review = reviewRequest.toEntity(reservation.getUser(),
        reservation.getRoom().getAccommodation(), reservation);
    reviewRepository.save(review);

    // 이미지 등록
    validateImageSize(images);
    addImages(review, images);

    // 숙소 총 평점 업데이트
    updateAccommodationRating(reservation.getRoom().getAccommodation(), 0, review.getUserRating(),
        review.getPetFriendlyRating());
  }

  public CustomReviewResponse<MyReviewResponse> getUsersReviews(Long userId, Long cursorId,
      Integer size) {
    // 해당 유저의 리뷰 내역만 조회 (리뷰 신고 수 20개 이상이면 조회 X)
    List<Review> reviews = reviewRepository.findByUserId(userId, cursorId, size + 1);

    List<MyReviewResponse> content = reviews.stream()
        .limit(size)
        .map(this::mapToMyReviewResponse)
        .toList();

    boolean hasNext = reviews.size() > size;
    Long cursor = hasNext ? reviews.get(size).getId() : null;

    return new CustomReviewResponse<>(content, cursor, hasNext);
  }

  public CustomReviewResponse<AccommodationReviewResponse> getAccommodationReviews(
      Long accommodationId,
      Long cursorId, Integer size) {
    // 해당 숙소의 리뷰 내역만 조회 (리뷰 신고 수 20개 이상이면 조회 X)
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

  @Transactional
  public void deleteReview(Long reviewId, Long userId) {
    // 리뷰 조회 및 권한 검증
    Review review = getReviewIfAuthorized(reviewId, userId);
    Accommodation accommodation = review.getAccommodation();
    double userRating = review.getUserRating();
    double petFriendlyRating = review.getPetFriendlyRating();

    // 리뷰에 포함된 모든 이미지 삭제
    deleteAllReviewImages(reviewId);

    // 리뷰 삭제
    reviewRepository.delete(review);

    // 숙소 총 평점 업데이트
    updateAccommodationRatingOnDelete(accommodation, userRating,
        petFriendlyRating);
  }

  @Transactional
  public void updateReview(Long userId, Long reviewId, List<MultipartFile> newImages,
      UpdateReviewRequest request) {
    // 리뷰 조회 및 권한 검증
    Review review = getReviewIfAuthorized(reviewId, userId);

    // 새로운 이미지 추가 후 최대 업로드 가능 개수(3장) 초과 여부 검증
    validateImageLimit(reviewId, request.getDeletedImageId(), newImages);

    // 요청된 이미지 삭제 수행
    deleteReviewImagesByIds(request.getDeletedImageId());

    // 새 이미지 업로드 및 저장
    addImages(review, newImages);

    // 리뷰 내용 업데이트
    updateReviewDetails(review, request);

    double oldRating = calculateReviewRating(review.getUserRating(), review.getPetFriendlyRating());

    // 숙소 총 평점 업데이트
    updateAccommodationRating(review.getAccommodation(), oldRating, request.getUserRating(),
        request.getPetRating());
  }

  /**
   * 호스트의 숙소 리뷰 목록 조회
   */
  public HostReviewResponse getHostReviews(Long hostId, Long cursorId, int pageSize) {
    Accommodation accommodation = findAccommodationByHostId(hostId);
    List<Review> reviews = getReviews(cursorId, pageSize, accommodation);

    boolean hasNext = reviews.size() > pageSize;

    if (hasNext) {
      reviews = reviews.subList(0, pageSize);
    }
    Long nextCursorId = hasNext ? reviews.get(reviews.size() - 1).getId() : null;

    List<Long> reviewIds = reviews.stream().map(Review::getId).toList(); // 리뷰 ID 추출

    // reviewIds에 속한 리뷰 이미지를 전부 조회
    List<ReviewImageProjection> reviewImageProjections = reviewImageRepository.findByReviewIds(
        reviewIds);

    // Map<ReviewId, List<ReviewImageUrl>> 형태의 Map 생성
    Map<Long, List<String>> reviewImagesMap = createReviewImagesMap(
        reviewIds, reviewImageProjections);

    // 각 정보를 ReviewContent로 변환
    List<ReviewContent> reviewContents = getReviewContents(reviews, reviewImagesMap);

    return new HostReviewResponse(reviewContents, nextCursorId, hasNext);
  }

  // 최대 이미지 개수(3장)를 초과하는지 검증
  private void validateImageSize(List<MultipartFile> images) {
    if (images.size() > 3) {
      throw new MeongnyangerangException(ErrorCode.MAX_IMAGE_LIMIT_EXCEEDED);
    }
  }

  // 이미지 등록
  private void addImages(Review review, List<MultipartFile> images) {
    images.stream()
        .filter(image -> !image.isEmpty())
        .map(imageService::storeImage)
        .map(url -> ReviewImage.builder().review(review).imageUrl(url).build())
        .forEach(reviewImageRepository::save);
  }

  // 리뷰를 작성할 수 있는지 검증
  private void validateReviewCreation(Long userId, Reservation reservation) {
    // 이미 작성된 리뷰인지 확인
    if (reviewRepository.existsByUserIdAndReservationId(userId, reservation.getId())) {
      throw new MeongnyangerangException(ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    // 예약한 사용자와 로그인한 사용자가 같은지 확인
    if (!Objects.equals(reservation.getUser().getId(), userId)) {
      throw new MeongnyangerangException(ErrorCode.INVALID_AUTHORIZED);
    }

    // 예약 생성 후 7일이 지나거나, 예약 상태가 COMPLETED 가 아닌 경우 예외 발생
    if (reservation.getCreatedAt().plusDays(7).isBefore(LocalDateTime.now()) ||
        reservation.getStatus() != ReservationStatus.COMPLETED) {
      throw new MeongnyangerangException(ErrorCode.REVIEW_CREATION_NOT_ALLOWED);
    }
  }

  // entity -> dto 변환
  private MyReviewResponse mapToMyReviewResponse(Review review) {
    List<ReviewImage> reviewImage = reviewImageRepository.findAllByReviewId(review.getId());

    List<ReviewImageResponse> reviewImageResponses = reviewImage.stream()
        .map(image -> new ReviewImageResponse(image.getId(), image.getImageUrl()))
        .toList();

    // 소숫점 한자리까지만 필요
    double totalRating =
        Math.round(((review.getUserRating() + review.getPetFriendlyRating()) / 2) * 10) / 10.0;

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    return MyReviewResponse.builder()
        .reviewId(review.getId())
        .accommodationName(review.getAccommodation().getName())
        .reviewImages(reviewImageResponses)
        .totalRating(totalRating)
        .content(review.getContent())
        .createdAt(review.getCreatedAt().format(dateFormatter))
        .build();
  }

  // entity -> dto 변환
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

  // 주어진 리뷰 ID로 리뷰를 조회하고, 해당 리뷰의 작성자인지 검증
  private Review getReviewIfAuthorized(Long reviewId, Long userId) {
    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.REVIEW_NOT_FOUND));

    if (!review.getUser().getId().equals(userId)) {
      throw new MeongnyangerangException(ErrorCode.REVIEW_NOT_AUTHORIZED);
    }
    return review;
  }

  // 특정 리뷰에 포함된 모든 이미지 삭제
  private void deleteAllReviewImages(Long reviewId) {
    List<ReviewImage> images = reviewImageRepository.findAllByReviewId(reviewId);
    List<String> imageUrls = images.stream().map(ReviewImage::getImageUrl).toList();

    imageService.deleteImagesAsync(imageUrls);

    reviewImageRepository.deleteAll(images);
  }

  // 이미지 삭제 요청 및 새로운 이미지 업로드 시, 최대 이미지 개수(3장)를 초과하는지 검증
  private void validateImageLimit(Long reviewId, List<Long> deletedImageIds,
      List<MultipartFile> newImages) {
    int existingCount = reviewImageRepository.findAllByReviewId(reviewId).size();
    int newCount = existingCount - deletedImageIds.size() + newImages.size();

    if (newCount > 3) {
      throw new MeongnyangerangException(ErrorCode.MAX_IMAGE_LIMIT_EXCEEDED);
    }
  }

  // 요청된 이미지 ID 목록을 찾아 삭제 (존재하지 않는 이미지 ID는 무시)
  private void deleteReviewImagesByIds(List<Long> deletedImageIds) {
    List<ReviewImage> images = reviewImageRepository.findAllById(deletedImageIds);

    List<String> imageUrls = images.stream().map(ReviewImage::getImageUrl).toList();
    imageService.deleteImagesAsync(imageUrls);

    reviewImageRepository.deleteAll(images);
  }

  // 리뷰 내용과 평점을 업데이트
  private void updateReviewDetails(Review review, UpdateReviewRequest request) {
    review.setContent(request.getContent());
    review.setUserRating(request.getUserRating());
    review.setPetFriendlyRating(request.getPetRating());
  }

  private Accommodation findAccommodationByHostId(Long hostId) {
    return accommodationRepository.findByHostId(hostId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.ACCOMMODATION_NOT_FOUND));
  }

  private List<Review> getReviews(Long cursorId, int pageSize, Accommodation accommodation) {
    Pageable pageable = PageRequest.of(0, pageSize + 1);
    // 다음 페이지 여부를 알기 위해 pageSize + 1

    return reviewRepository.findByAccommodationIdWithCursor(
        accommodation.getId(), cursorId, pageable);
  }

  private static Map<Long, List<String>> createReviewImagesMap(
      List<Long> reviewIds, List<ReviewImageProjection> reviewImageProjections
  ) {
    Map<Long, List<String>> reviewImagesMap = new HashMap<>();
    reviewIds.forEach(id -> reviewImagesMap.put(id, new ArrayList<>()));

    reviewImageProjections.forEach(projection ->
        reviewImagesMap.get(projection.getReviewId()).add(projection.getImageUrl()));

    return reviewImagesMap;
  }

  private List<ReviewContent> getReviewContents(
      List<Review> pagedReviews, Map<Long, List<String>> reviewImagesMap
  ) {
    return pagedReviews.stream()
        .map(review -> ReviewContent.of(review, reviewImagesMap.get(review.getId())))
        .toList();
  }

  private void updateAccommodationRating(Accommodation accommodation, double oldRating,
      double userRating, double petFriendlyRating) {

    double existingTotalRating = accommodation.getTotalRating();
    int reviewCount = reviewRepository.countByAccommodationId(accommodation.getId());

    double newReviewRating = calculateReviewRating(userRating, petFriendlyRating);

    double newTotalRating;
    if (oldRating == 0) {  // 리뷰 등록
      newTotalRating = ((existingTotalRating * (reviewCount - 1)) + newReviewRating) / reviewCount;
    } else {  // 리뷰 수정
      newTotalRating =
          ((existingTotalRating * reviewCount) - oldRating + newReviewRating) / reviewCount;
    }

    accommodation.setTotalRating(Math.round(newTotalRating * 10) / 10.0);
  }

  private void updateAccommodationRatingOnDelete(Accommodation accommodation, double userRating,
      double petFriendlyRating) {
    int reviewCount = reviewRepository.countByAccommodationId(accommodation.getId());

    if (reviewCount == 0) {
      accommodation.setTotalRating(0.0);
      return;
    }

    double existingTotalRating = accommodation.getTotalRating();
    double removedReviewRating = calculateReviewRating(userRating, petFriendlyRating);

    double newTotalRating = Math.round(
        ((existingTotalRating * (reviewCount + 1) - removedReviewRating) / reviewCount) * 10)
        / 10.0;

    accommodation.setTotalRating(newTotalRating);
  }

  private double calculateReviewRating(double userRating, double petFriendlyRating) {
    return Math.round(((userRating + petFriendlyRating) / 2) * 10) / 10.0;
  }
}
