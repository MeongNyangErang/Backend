package com.meongnyangerang.meongnyangerang.component;

import com.meongnyangerang.meongnyangerang.domain.AccommodationRoomDocument;
import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AllowPet;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacility;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacility;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.room.facility.Hashtag;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomFacility;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacility;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccommodationRoomMapper {

  public AccommodationRoomDocument toDocument(
      Accommodation accommodation,
      Room room,
      List<AccommodationFacility> accFacilities,
      List<AccommodationPetFacility> accPetFacilities,
      List<RoomFacility> roomFacilities,
      List<RoomPetFacility> roomPetFacilities,
      List<Hashtag> hashtags,
      List<AllowPet> allowPets
  ) {
    return AccommodationRoomDocument.builder()
        .id(accommodation.getId() + "_" + room.getId())
        .accommodationId(accommodation.getId())
        .roomId(room.getId())
        .accommodationName(accommodation.getName())
        .roomName(room.getName())
        .address(accommodation.getAddress())
        .thumbnailUrl(accommodation.getThumbnailUrl())
        .accommodationType(accommodation.getType())
        .totalRating(accommodation.getTotalRating())
        .price(room.getPrice())
        .standardPeopleCount(room.getStandardPeopleCount())
        .maxPeopleCount(room.getMaxPeopleCount())
        .standardPetCount(room.getStandardPetCount())
        .maxPetCount(room.getMaxPetCount())
        .accommodationFacilities(extractEnumNames(accFacilities, AccommodationFacility::getType))
        .accommodationPetFacilities(extractEnumNames(accPetFacilities, AccommodationPetFacility::getType))
        .roomFacilities(extractEnumNames(roomFacilities, RoomFacility::getType))
        .roomPetFacilities(extractEnumNames(roomPetFacilities, RoomPetFacility::getType))
        .hashtags(extractEnumNames(hashtags, Hashtag::getType))
        .allowPets(extractEnumNames(allowPets, AllowPet::getPetType))
        .build();
  }
  private <T> List<String> extractEnumNames(List<T> list, Function<T, Enum<?>> extractor) {

//    if (list == null || list.isEmpty()) {
//      return Collections.emptyList();
//    }
    return list.stream()
        .map(extractor)
        .map(Enum::name)
        .toList();
  }
}

