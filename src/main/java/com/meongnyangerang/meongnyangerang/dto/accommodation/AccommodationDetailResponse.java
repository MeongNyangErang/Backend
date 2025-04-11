package com.meongnyangerang.meongnyangerang.dto.accommodation;

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
  private String type;
  private String thumbnailUrl;
  private List<String> accommodationImages;
  private Double totalRating;
  private List<String> accommodationFacility;
  private List<String> accommodationPetFacility;
  private List<String> allowPet;
  private Double latitude;
  private Double longitude;
  private List<ReviewSummary> review;
  private List<RoomDetail> rooms;

  @Builder
  @Getter
  public static class ReviewSummary {
    private Double reviewRating;
    private String content;
    private LocalDateTime createdAt;
  }

  @Builder
  @Getter
  public static class RoomDetail {
    private Long roomId;
    private String roomName;
    private List<String> roomImageUrl;
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
  }
}

