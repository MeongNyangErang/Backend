package com.meongnyangerang.meongnyangerang.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccommodationReviewResponse {

  private String roomName;
  private String nickname;
  private String profileImageUrl;
  private Double totalRating;
  private String content;
  private List<String> reviewImages;
  private String createdAt;

}
