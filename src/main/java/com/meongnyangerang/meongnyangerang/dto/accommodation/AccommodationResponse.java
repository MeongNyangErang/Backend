package com.meongnyangerang.meongnyangerang.dto.accommodation;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationImage;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AllowPet;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacility;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacility;
import java.util.List;

public record AccommodationResponse(
    Long accommodationId,
    String name,
    String type,
    String address,
    String detailedAddress,
    String description,
    Double latitude,
    Double longitude,
    String thumbnailUrl,
    List<String> facilityTypes,
    List<String> petFacilityTypes,
    List<String> allowPetTypes,
    List<String> additionalImages
) {

  public static AccommodationResponse of(
      Accommodation accommodation,
      List<AccommodationFacility> facilities,
      List<AccommodationPetFacility> petFacilities,
      List<AllowPet> allowedPets,
      List<AccommodationImage> additionalImages
  ) {
    List<String> facilityDescriptions = facilities.stream()
        .map(facility -> facility.getType().getValue())
        .toList();

    List<String> petFacilityDescriptions = petFacilities.stream()
        .map(petFacility -> petFacility.getType().getValue())
        .toList();

    List<String> allowPetDescriptions = allowedPets.stream()
        .map(allowPet -> allowPet.getPetType().getValue())
        .toList();

    List<String> imageUrls = additionalImages.stream()
        .map(AccommodationImage::getImageUrl)
        .toList();

    return new AccommodationResponse(
        accommodation.getId(),
        accommodation.getName(),
        accommodation.getType().getValue(),
        accommodation.getAddress(),
        accommodation.getDetailedAddress(),
        accommodation.getDescription(),
        accommodation.getLatitude(),
        accommodation.getLongitude(),
        accommodation.getThumbnailUrl(),
        facilityDescriptions,
        petFacilityDescriptions,
        allowPetDescriptions,
        imageUrls
    );
  }
}
