package com.meongnyangerang.meongnyangerang.dto.room;

import com.meongnyangerang.meongnyangerang.domain.room.Room;

public record RoomSummaryResponse(
    Long roomId,
    String name,
    String description,
    String thumbnailUrl,
    Long price,
    Integer standardPeopleCount,
    Integer maxPeopleCount,
    Integer standardPetCount,
    Integer maxPetCount
) {

  public static RoomSummaryResponse of(Room room) {
    return new RoomSummaryResponse(
        room.getId(),
        room.getName(),
        room.getDescription(),
        room.getImageUrl(),
        room.getPrice(),
        room.getStandardPeopleCount(),
        room.getMaxPeopleCount(),
        room.getStandardPetCount(),
        room.getMaxPetCount()
    );
  }
}
