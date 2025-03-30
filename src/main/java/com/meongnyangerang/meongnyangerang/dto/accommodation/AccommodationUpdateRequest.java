package com.meongnyangerang.meongnyangerang.dto.accommodation;

import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.PetType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacilityType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacilityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record AccommodationUpdateRequest(

    @NotNull(message = "숙소 ID를 요청해 주세요.")
    Long accommodationId,

    @NotBlank(message = "숙소 이름을 입력해 주세요.")
    String name,

    @NotNull(message = "숙소 유형을 선택해 주세요.")
    AccommodationType type,

    @NotBlank(message = "숙소 주소를 입력해 주세요.")
    String address,

    String detailedAddress,

    String description,

    @NotNull(message = "숙소의 위치 정보를 제공해 주세요.")
    Double latitude,

    @NotNull(message = "숙소의 위치 정보를 제공해 주세요.")
    Double longitude,

    @NotEmpty(message = "대표 이미지의 URL은 비어 있을 수 없습니다.")
    String oldThumbnailUrl,

    List<String> oldAdditionalImageUrls,

    @NotEmpty(message = "숙소 편의시설을 하나 이상 선택해 주세요.")
    List<AccommodationFacilityType> facilityTypes,

    @NotEmpty(message = "반려동물 편의시설을 하나 이상 선택해 주세요.")
    List<AccommodationPetFacilityType> petFacilityTypes,

    @NotEmpty(message = "허용 가능한 반려동물 유형을 하나 이상 선택해 주세요.")
    List<PetType> allowPetTypes
) {

  public AccommodationUpdateRequest {
    oldAdditionalImageUrls = Optional.ofNullable(oldAdditionalImageUrls)
        .orElseGet(ArrayList::new);
  }

  public static AccommodationUpdateRequest of(
      Long accommodationId,
      String name,
      AccommodationType type,
      String address,
      String detailedAddress,
      String description,
      Double latitude,
      Double longitude,
      String oldThumbnailUrl,
      List<String> oldAdditionalImageUrls,
      List<AccommodationFacilityType> facilityTypes,
      List<AccommodationPetFacilityType> petFacilityTypes,
      List<PetType> allowPetTypes
  ) {
    return new AccommodationUpdateRequest(
        accommodationId, name, type, address, detailedAddress,
        description, latitude, longitude, oldThumbnailUrl,
        oldAdditionalImageUrls, facilityTypes, petFacilityTypes, allowPetTypes
    );
  }
}