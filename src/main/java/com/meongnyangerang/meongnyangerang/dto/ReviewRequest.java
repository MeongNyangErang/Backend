package com.meongnyangerang.meongnyangerang.dto;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.reservation.Reservation;
import com.meongnyangerang.meongnyangerang.domain.review.Review;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewRequest {

  @NotNull
  private Long reservationId;

  @NotNull
  @Min(value = 0)
  @Max(value = 5)
  private Double userRating;

  @NotNull
  @Min(value = 0)
  @Max(value = 5)
  private Double petFriendlyRating;

  private String content;

  public Review toEntity(User user, Accommodation accommodation, Reservation reservation) {
    return Review.builder()
        .user(user)
        .accommodation(accommodation)
        .reservation(reservation)
        .content(content)
        .userRating(userRating)
        .petFriendlyRating(petFriendlyRating)
        .reportCount(0)
        .build();
  }
}
