package com.meongnyangerang.meongnyangerang.dto.room;

import com.meongnyangerang.meongnyangerang.domain.room.Room;

public record RoomSummary(
    Long roomId,
    String name,
    String description,
    String imageUrl,
    Long price,
    Integer standardPeopleCount,
    Integer maxPeopleCount,
    Integer standardPetCount,
    Integer maxPetCount
) {

  public static RoomSummary of(Room room) {
    return new RoomSummary(
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
