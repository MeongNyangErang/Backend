package com.meongnyangerang.meongnyangerang.dto.room;

import com.meongnyangerang.meongnyangerang.domain.room.Room;
import java.util.List;

public record RoomListResponse(
    List<RoomSummary> content,
    Long nextCursor,
    Boolean hasNext
) {

  public static RoomListResponse of(List<Room> rooms, Long nextCursor, boolean hasNext) {
    List<RoomSummary> content = rooms.stream()
        .map(RoomSummary::of)
        .toList();

    return new RoomListResponse(content, nextCursor, hasNext);
  }
}
