package com.meongnyangerang.meongnyangerang.dto.room;

import com.meongnyangerang.meongnyangerang.domain.room.Room;
import java.util.List;

public record RoomListResponse(
    List<RoomContent> content,
    Long nextCursor,
    Boolean hasNext
) {

  public static RoomListResponse of(List<Room> rooms, Long nextCursor, boolean hasNext) {
    List<RoomContent> content = rooms.stream()
        .map(RoomContent::of)
        .toList();

    return new RoomListResponse(content, nextCursor, hasNext);
  }
}
