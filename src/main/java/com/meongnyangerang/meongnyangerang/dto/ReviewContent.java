package com.meongnyangerang.meongnyangerang.dto;

import com.meongnyangerang.meongnyangerang.domain.review.Review;
import java.time.LocalDateTime;
import java.util.List;

public record ReviewContent(
    String nickname,
    Long roomId,
    Long reviewId,
    String roomName,
    Double totalRating,
    String reviewContent,
    List<String> imageUrls,
    LocalDateTime createdAt
) {

  public static ReviewContent of(Review review, List<String> imageUrls) {
    Double totalRating = (review.getUserRating() + review.getPetFriendlyRating()) / 2;

    return new ReviewContent(
        review.getUser().getNickname(),
        review.getReservation().getRoom().getId(),
        review.getId(),
        review.getReservation().getRoomName(),
        totalRating,
        review.getContent(),
        imageUrls,
        review.getCreatedAt()
    );
  }
}
