package com.meongnyangerang.meongnyangerang.dto.room;

import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.room.facility.Hashtag;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomFacility;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacility;
import java.time.LocalTime;
import java.util.List;

public record RoomResponse(
    Long roomId,
    String name,
    String description,
    Integer standardPeopleCount,
    Integer maxPeopleCount,
    Integer standardPetCount,
    Integer maxPetCount,
    Long price,
    Long extraPeopleFee,
    Long extraPetFee,
    Long extraFee,
    LocalTime checkInTime,
    LocalTime checkOutTime,
    String thumbnailUrl,
    List<String> facilityTypes,
    List<String> petFacilityTypes,
    List<String> hashtagTypes
) {

  public static RoomResponse of(
      Room room,
      List<RoomFacility> facilities,
      List<RoomPetFacility> petFacilities,
      List<Hashtag> hashtags
  ) {
    List<String> facilityValues = facilities.stream()
        .map(facility -> facility.getType().getValue())
        .toList();

    List<String> petFacilityValues = petFacilities.stream()
        .map(petFacility -> petFacility.getType().getValue())
        .toList();

    List<String> hashtagsValues = hashtags.stream()
        .map(hashtag -> hashtag.getType().getValue())
        .toList();

    return new RoomResponse(
        room.getId(),
        room.getName(),
        room.getDescription(),
        room.getStandardPeopleCount(),
        room.getMaxPeopleCount(),
        room.getStandardPetCount(),
        room.getMaxPetCount(),
        room.getPrice(),
        room.getExtraPeopleFee(),
        room.getExtraPetFee(),
        room.getExtraFee(),
        room.getCheckInTime(),
        room.getCheckOutTime(),
        room.getImageUrl(),
        facilityValues,
        petFacilityValues,
        hashtagsValues
    );
  }
}
