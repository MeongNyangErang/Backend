package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.image.ImageDeletionQueue;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.room.facility.Hashtag;
import com.meongnyangerang.meongnyangerang.domain.room.facility.HashtagType;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomFacility;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomFacilityType;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacility;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacilityType;
import com.meongnyangerang.meongnyangerang.dto.room.RoomCreateRequest;
import com.meongnyangerang.meongnyangerang.dto.room.RoomListResponse;
import com.meongnyangerang.meongnyangerang.dto.room.RoomResponse;
import com.meongnyangerang.meongnyangerang.dto.room.RoomUpdateRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.room.HashtagRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomPetFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

  private final RoomRepository roomRepository;
  private final AccommodationRepository accommodationRepository;
  private final RoomFacilityRepository roomFacilityRepository;
  private final RoomPetFacilityRepository roomPetFacilityRepository;
  private final HashtagRepository hashtagRepository;
  private final ImageService imageService;

  /**
   * 객실 등록
   */
  public void createRoom(Long hostId, RoomCreateRequest request, MultipartFile thumbnail) {
    Accommodation accommodation = findAccommodationByHostId(hostId);
    String thumbnailUrl = imageService.storeImage(thumbnail);

    Room room = request.toEntity(accommodation, thumbnailUrl);
    roomRepository.save(room);
  }

  /**
   * 객실 목록 조회
   */
  public RoomListResponse getRoomList(Long hostId, Long cursorId, int pageSize) {
    Accommodation accommodation = findAccommodationByHostId(hostId);
    Pageable pageable = PageRequest.of(0, pageSize + 1);
    // 다음 페이지 여부를 알기 위해 pageSize + 1

    List<Room> rooms = roomRepository.findByAccommodationIdWithCursor(
        accommodation.getId(), cursorId, pageable);

    boolean hasNext = rooms.size() > pageSize;

    if (hasNext) {
      rooms = rooms.subList(0, pageSize);
    }
    Long nextCursorId = hasNext ? rooms.get(rooms.size() - 1).getId() : null;

    return RoomListResponse.of(rooms, nextCursorId, hasNext);
  }

  /**
   * 객실 상세 조회
   */
  public RoomResponse getRoom(Long hostId, Long roomId) {
    Room room = getAuthorizedRoom(hostId, roomId);
    return createRoomResponse(room);
  }

  /**
   * 객실 수정
   */
  @Transactional
  public RoomResponse updateRoom(Long hostId, RoomUpdateRequest request, MultipartFile newThumbnail) {
    Room room = getAuthorizedRoom(hostId, request.roomId());
    String newThumbnailUrl = null;

    try {
      if (newThumbnail != null && !newThumbnail.isEmpty()) {
        newThumbnailUrl = imageService.storeImage(newThumbnail);
        imageService.registerImagesForDeletion(room.getImageUrl());
      }

      Room updatedRoom = room.updateRoom(
          request,
          newThumbnailUrl != null ? newThumbnailUrl : room.getImageUrl()
      );
      List<RoomFacility> updatedFacilities = updateFacilities(request.facilityTypes(), room);
      List<RoomPetFacility> updatedPetFacilities = updatePetFacilities(
          request.petFacilityTypes(), room);
      List<Hashtag> updatedHashtags = updateHashtags(request.hashtagTypes(), room);

      return RoomResponse.of(updatedRoom, updatedFacilities, updatedPetFacilities, updatedHashtags);
    } catch (Exception e) {
      log.error("객실 업데이트 실행: {}", e.getMessage(), e);
      rollbackProcess(newThumbnailUrl);

      throw new MeongnyangerangException(ErrorCode.ROOM_UPDATE_FAILED);
    }
  }

  /**
   * 객실 삭제
   */
  @Transactional
  public void deleteRoom(Long hostId, Long roomId) {
    Room room = getAuthorizedRoom(hostId, roomId);
    hashtagRepository.deleteAllByRoomId(roomId);
    roomPetFacilityRepository.deleteAllByRoomId(roomId);
    roomFacilityRepository.deleteAllByRoomId(roomId);
    roomRepository.delete(room);
  }

  private List<RoomFacility> updateFacilities(List<RoomFacilityType> newFacilityTypes, Room room) {
    roomFacilityRepository.deleteAllByRoomId(room.getId());
    return saveRoomFacilities(newFacilityTypes, room);
  }

  private List<RoomFacility> saveRoomFacilities(List<RoomFacilityType> facilityTypes, Room room) {
    List<RoomFacility> facilities = facilityTypes.stream()
        .map(facilityType -> RoomFacility.builder()
            .room(room)
            .type(facilityType)
            .build())
        .toList();

    return roomFacilityRepository.saveAll(facilities);
  }

  private List<RoomPetFacility> updatePetFacilities(
      List<RoomPetFacilityType> newPetFacilityTypes, Room room
  ) {
    roomPetFacilityRepository.deleteAllByRoomId(room.getId());
    return saveRoomPetFacilities(newPetFacilityTypes, room);
  }

  private List<RoomPetFacility> saveRoomPetFacilities(
      List<RoomPetFacilityType> petFacilityTypes, Room room
  ) {
    List<RoomPetFacility> petFacilities = petFacilityTypes.stream()
        .map(petFacilityType -> RoomPetFacility.builder()
            .room(room)
            .type(petFacilityType)
            .build())
        .toList();

    return roomPetFacilityRepository.saveAll(petFacilities);
  }

  private List<Hashtag> updateHashtags(List<HashtagType> newHashtagTypes, Room room) {
    hashtagRepository.deleteAllByRoomId(room.getId());
    return saveHashtags(newHashtagTypes, room);
  }

  private List<Hashtag> saveHashtags(List<HashtagType> hashtagTypes, Room room) {
    List<Hashtag> hashtags = hashtagTypes.stream()
        .map(hashtag -> Hashtag.builder()
            .room(room)
            .type(hashtag)
            .build())
        .toList();

    return hashtagRepository.saveAll(hashtags);
  }

  private RoomResponse createRoomResponse(Room room) {
    Long roomId = room.getId();
    List<RoomFacility> facilities = roomFacilityRepository.findAllByRoomId(roomId);
    List<RoomPetFacility> petFacilities = roomPetFacilityRepository.findAllByRoomId(roomId);
    List<Hashtag> hashtags = hashtagRepository.findAllByRoomId(roomId);

    return RoomResponse.of(room, facilities, petFacilities, hashtags);
  }

  private void rollbackProcess(String newImageUrl) {
    if (newImageUrl != null) {
      imageService.deleteImage(newImageUrl); // 새로 업로드한 이미지 삭제
      log.info("새로운 이미지 저장 롤백 -> S3에서 제거: {}", newImageUrl);
    }
  }

  private Room getAuthorizedRoom(Long hostId, Long roomId) {
    Accommodation accommodation = findAccommodationByHostId(hostId);
    Room room = findRoomById(roomId);
    validateRoomAuthorization(accommodation, room);
    return room;
  }

  private Accommodation findAccommodationByHostId(Long hostId) {
    return accommodationRepository.findByHostId(hostId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.ACCOMMODATION_NOT_FOUND));
  }

  private Room findRoomById(Long roomId) {
    return roomRepository.findById(roomId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.ROOM_NOT_FOUND));
  }

  private void validateRoomAuthorization(Accommodation accommodation, Room room) {
    if (!accommodation.getId().equals(room.getAccommodation().getId())) {
      throw new MeongnyangerangException(ErrorCode.INVALID_AUTHORIZED);
    }
  }
}
