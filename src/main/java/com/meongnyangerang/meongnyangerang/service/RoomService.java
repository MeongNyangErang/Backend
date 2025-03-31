package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.dto.room.RoomCreateRequest;
import com.meongnyangerang.meongnyangerang.dto.room.RoomListResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.RoomRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

  private final RoomRepository roomRepository;
  private final AccommodationRepository accommodationRepository;
  private final ImageService imageService;

  /**
   * 객실 등록
   */
  public void createRoom(Long hostId, RoomCreateRequest request, MultipartFile images) {
    Accommodation accommodation = findAccommodationByHostId(hostId);
    String imageUrl = imageService.storeImage(images);

    Room room = request.toEntity(accommodation, imageUrl);
    roomRepository.save(room);
  }

  /**
   * 객실 목록 조회
   */
  public RoomListResponse getRoomList(Long hostId, Long cursorId, int pageSize) {
    Accommodation accommodation = findAccommodationByHostId(hostId);
    Pageable pageable = PageRequest.of(
        0, pageSize + 1, Sort.by(Sort.Direction.DESC, "id"));
    // 다음 페이지 여부를 알기 위해 pageSize + 1

    List<Room> rooms = roomRepository.findRoomsWithCursor(
        accommodation.getId(), cursorId, pageable);

    boolean hasNext = rooms.size() > pageSize;

    if (hasNext) {
      rooms = rooms.subList(0, pageSize);
    }
    Long nextCursorId = hasNext ? rooms.get(rooms.size() - 1).getId() : null;

    return RoomListResponse.of(rooms, nextCursorId, hasNext);
  }

  private Accommodation findAccommodationByHostId(Long hostId) {
    return accommodationRepository.findByHostId(hostId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.ACCOMMODATION_NOT_FOUND));
  }
}
