package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.review.Review;
import com.meongnyangerang.meongnyangerang.domain.review.ReviewImage;
import com.meongnyangerang.meongnyangerang.repository.ReviewImageRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewDeletionService {

  private final ReviewRepository reviewRepository;
  private final ReviewImageRepository reviewImageRepository;
  private final AccommodationRoomSearchService accommodationRoomSearchService;
  private final ImageService imageService;
  private final RoomRepository roomRepository;

  public void deleteReviewCompletely(Review review) {
    deleteAllReviewImages(review.getId());

    Accommodation accommodation = review.getAccommodation();
    double userRating = review.getUserRating();
    double petFriendlyRating = review.getPetFriendlyRating();

    reviewRepository.delete(review);
    updateAccommodationRatingOnDelete(accommodation, userRating, petFriendlyRating);
    updateElasticsearchDocument(accommodation);
  }

  private void deleteAllReviewImages(Long reviewId) {
    List<ReviewImage> images = reviewImageRepository.findAllByReviewId(reviewId);
    List<String> imageUrls = images.stream().map(ReviewImage::getImageUrl).toList();

    imageService.deleteImagesAsync(imageUrls);

    reviewImageRepository.deleteAll(images);
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

  private void updateElasticsearchDocument(Accommodation accommodation) {
    accommodationRoomSearchService.updateAllRooms(accommodation,
        roomRepository.findAllByAccommodationId(accommodation.getId()));
    accommodationRoomSearchService.updateAccommodationTotalRating(accommodation,
        accommodation.getTotalRating());
  }
}
