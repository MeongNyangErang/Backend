package com.meongnyangerang.meongnyangerang.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class WishlistResponse {

  private Long wishlistId;
  private Long accommodationId;
  private String accommodationName;
  private String thumbnailImageUrl;
  private String address;
  private Double totalRating;
}
