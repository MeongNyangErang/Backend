package com.meongnyangerang.meongnyangerang.domain.accommodation;

import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccommodationDocument {

  private Long id;

  private String name;

  private String thumbnailUrl;

  private Long price;

  private Double totalRating;

  private Set<String> accommodationPetFacilities;

  private Set<String> roomPetFacilities;

  private Set<String> allowedPetTypes;
}
