package com.meongnyangerang.meongnyangerang.dto.accommodation;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.PetType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacilityType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacilityType;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AccommodationCreateRequest(

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

    @NotEmpty(message = "숙소 편의시설을 하나 이상 선택해 주세요.")
    List<AccommodationFacilityType> facilityTypes,

    @NotEmpty(message = "반려동물 편의시설을 하나 이상 선택해 주세요.")
    List<AccommodationPetFacilityType> petFacilityTypes,

    @NotEmpty(message = "허용 가능한 반려동물 유형을 하나 이상 선택해 주세요.")
    List<PetType> allowPetTypes
) {

  public Accommodation toEntity(Host host, String thumbnailUrl) {
    return Accommodation.builder()
        .host(host)
        .name(this.name)
        .description(this.description)
        .address(this.address)
        .detailedAddress(this.detailedAddress)
        .latitude(this.latitude)
        .longitude(this.longitude)
        .totalRating(0.0)
        .viewCount(0L)
        .type(this.type)
        .thumbnailUrl(thumbnailUrl)
        .build();
  }
}