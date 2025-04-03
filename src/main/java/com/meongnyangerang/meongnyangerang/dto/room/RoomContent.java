package com.meongnyangerang.meongnyangerang.dto.room;

import com.meongnyangerang.meongnyangerang.domain.room.Room;

public record RoomContent(
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

  public static RoomContent of(Room room) {
    return new RoomContent(
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
