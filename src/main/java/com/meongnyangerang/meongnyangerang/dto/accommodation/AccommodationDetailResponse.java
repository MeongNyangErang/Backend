package com.meongnyangerang.meongnyangerang.dto.accommodation;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationImage;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AllowPet;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacility;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacility;
import com.meongnyangerang.meongnyangerang.domain.review.Review;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class AccommodationDetailResponse {
  private Long accommodationId;
  private String name;
  private String description;
  private String address;
  private String detailedAddress;
  private boolean isWishlisted;
  private String type;
  private String thumbnailUrl;
  private List<String> accommodationImageUrls;
  private Double totalRating;
  private List<String> accommodationFacilities;
  private List<String> accommodationPetFacilities;
  private List<String> allowedPets;
  private Double latitude;
  private Double longitude;
  private List<ReviewSummary> reviews;
  private List<RoomDetail> roomDetails;

  public static AccommodationDetailResponse of(
      Accommodation accommodation,
      List<AccommodationImage> images,
      List<AccommodationFacility> facilities,
      List<AccommodationPetFacility> petFacilities,
      List<AllowPet> allowPets,
      List<Review> reviews,
      List<Room> rooms
  ) {
    return AccommodationDetailResponse.builder()
        .accommodationId(accommodation.getId())
        .name(accommodation.getName())
        .description(accommodation.getDescription())
        .address(accommodation.getAddress())
        .detailedAddress(accommodation.getDetailedAddress())
        .type(accommodation.getType().getValue())
        .thumbnailUrl(accommodation.getThumbnailUrl())
        .accommodationImageUrls(images.stream().map(AccommodationImage::getImageUrl).toList())
        .totalRating(accommodation.getTotalRating())
        .accommodationFacilities(facilities.stream().map(f -> f.getType().getValue()).toList())
        .accommodationPetFacilities(petFacilities.stream().map(p -> p.getType().getValue()).toList())
        .allowedPets(allowPets.stream().map(p -> p.getPetType().getValue()).toList())
        .reviews(reviews.stream().map(ReviewSummary::of).toList())
        .roomDetails(rooms.stream().map(RoomDetail::of).toList())
        .latitude(accommodation.getLatitude())
        .longitude(accommodation.getLongitude())
        .build();
  }

  @Builder
  @Getter
  public static class ReviewSummary {
    private Long reviewId;
    private Double reviewRating;
    private String content;
    private LocalDateTime createdAt;

    public static ReviewSummary of(Review review) {
      double rounded = Math.round((review.getUserRating() + review.getPetFriendlyRating()) / 2.0 * 10) / 10.0;
      return ReviewSummary.builder()
          .reviewId(review.getId())
          .reviewRating(rounded)
          .content(review.getContent())
          .createdAt(review.getCreatedAt())
          .build();
    }
  }

  @Builder
  @Getter
  public static class RoomDetail {
    private Long roomId;
    private String roomName;
    private String roomImageUrl;
    private Integer price;
    private Integer standardPeopleCount;
    private Integer maxPeopleCount;
    private Integer standardPetCount;
    private Integer maxPetCount;
    private Integer extraPeopleFee;
    private Integer extraPetFee;
    private Integer extraFee;
    private String checkInTime;
    private String checkOutTime;

    public static RoomDetail of(Room room) {
      return RoomDetail.builder()
          .roomId(room.getId())
          .roomName(room.getName())
          .roomImageUrl(room.getImageUrl())
          .price(room.getPrice().intValue())
          .standardPeopleCount(room.getStandardPeopleCount())
          .maxPeopleCount(room.getMaxPeopleCount())
          .standardPetCount(room.getStandardPetCount())
          .maxPetCount(room.getMaxPetCount())
          .extraPeopleFee(room.getExtraPeopleFee().intValue())
          .extraPetFee(room.getExtraPetFee().intValue())
          .extraFee(room.getExtraFee().intValue())
          .checkInTime(room.getCheckInTime().toString())
          .checkOutTime(room.getCheckOutTime().toString())
          .build();
    }
  }
}

