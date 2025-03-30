package com.meongnyangerang.meongnyangerang.dto.room;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.room.facility.Hashtag;
import com.meongnyangerang.meongnyangerang.domain.room.facility.HashtagType;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomFacilityType;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacilityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

public record RoomCreateRequest(

    @NotNull(message = "숙소 ID를 요청해 주세요.")
    Long accommodationId,

    @NotBlank(message = "객실 이름을 입력해 주세요.")
    String name,

    String description,

    @NotNull(message = "기준 인원을 설정해 주세요.")
    Integer standardPeopleCount,

    @NotNull(message = "최대 인원을 설정해 주세요.")
    Integer maxPeopleCount,

    @NotNull(message = "기준 반려동물 수를 설정해 주세요.")
    Integer standardPetCount,

    @NotNull(message = "최대 반려동물 수를 설정해 주세요.")
    Integer maxPetCount,

    @NotNull(message = "가격을 설정해 주세요.")
    Long price,

    Long extraPeopleFee,

    Long extraPetFee,

    @NotNull(message = "추가 요금을 설정해 주세요.")
    Long extraFee,

    @NotNull(message = "체크인 시간을 설정해 주세요.")
    @DateTimeFormat(pattern = "HH:mm")
    LocalTime checkInTime,

    @NotNull(message = "체크아웃 시간을 설정해 주세요.")
    @DateTimeFormat(pattern = "HH:mm")
    LocalTime checkOutTime,

    @NotEmpty(message = "해시태그를 하나 이상 선택해 주세요.")
    List<HashtagType> hashtagTypes,

    @NotEmpty(message = "숙소 편의시설을 하나 이상 선택해 주세요.")
    List<RoomFacilityType> facilityTypes,

    @NotEmpty(message = "반려동물 편의시설을 하나 이상 선택해 주세요.")
    List<RoomPetFacilityType> petFacilityTypes
) {

    public Room toEntity(Accommodation accommodation, String imageUrl) {
        return Room.builder()
            .accommodation(accommodation)
            .name(name)
            .description(description)
            .standardPeopleCount(standardPeopleCount)
            .maxPeopleCount(maxPeopleCount)
            .standardPetCount(standardPetCount)
            .maxPetCount(maxPetCount)
            .imageUrl(imageUrl)
            .price(price)
            .extraPeopleFee(extraPeopleFee)
            .extraPetFee(extraPetFee)
            .extraFee(extraFee)
            .checkInTime(checkInTime)
            .checkOutTime(checkOutTime)
            .build();
    }
}
