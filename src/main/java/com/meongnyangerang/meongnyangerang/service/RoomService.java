package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ROOM_NOT_FOUND;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.room.facility.Hashtag;
import com.meongnyangerang.meongnyangerang.domain.room.facility.HashtagType;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomFacility;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomFacilityType;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacility;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacilityType;
import com.meongnyangerang.meongnyangerang.dto.room.RoomSummaryResponse;
import com.meongnyangerang.meongnyangerang.dto.room.RoomCreateRequest;
import com.meongnyangerang.meongnyangerang.dto.room.RoomResponse;
import com.meongnyangerang.meongnyangerang.dto.room.RoomUpdateRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.ReservationRepository;
import com.meongnyangerang.meongnyangerang.repository.ReservationSlotRepository;
import com.meongnyangerang.meongnyangerang.repository.room.HashtagRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomPetFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final ReservationRepository reservationRepository;
  private final ReservationSlotRepository reservationSlotRepository;
  private final ImageService imageService;
  private final AccommodationRoomSearchService searchService;

  private static final int ROOM_CREATION_LIMIT_COUNT = 20;

  /**
   * 객실 등록
   */
  @Transactional
  public void createRoom(Long hostId, RoomCreateRequest request, MultipartFile thumbnail) {
    Accommodation accommodation = findAccommodationByHostId(hostId);
    validateCreatableCount(accommodation.getId());

    String thumbnailUrl = imageService.storeImage(thumbnail);
    Room room = request.toEntity(accommodation, thumbnailUrl);

    roomRepository.save(room);
    saveRoomFacilities(request.facilityTypes(), room);
    saveRoomPetFacilities(request.petFacilityTypes(), room);
    saveHashtags(request.hashtagTypes(), room);
    searchService.save(accommodation, room); // Elasticsearch 색인 저장

    if (roomRepository.findAllByAccommodationId(accommodation.getId()).size() > 1) {
      searchService.updateAccommodationDocument(accommodation);
    } else {
      searchService.indexAccommodationDocument(accommodation, room);
    }
  }

  /**
   * 객실 목록 조회
   */
  public List<RoomSummaryResponse> getRoomList(Long hostId) {
    Accommodation accommodation = findAccommodationByHostId(hostId);
    List<Room> rooms = roomRepository.findAllByAccommodationId(accommodation.getId());

    return rooms.stream()
        .map(RoomSummaryResponse::of)
        .toList();
  }

  /**
   * 객실 상세 조회(호스트 전용)
   */
  public RoomResponse getRoom(Long hostId, Long roomId) {
    Room room = getAuthorizedRoom(hostId, roomId);
    return createRoomResponse(room);
  }

  /**
   * 객실 상세 조회(모든 사용자가 사용)
   */
  @Transactional(readOnly = true)
  public RoomResponse getRoomDetail(Long roomId) {
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new MeongnyangerangException(ROOM_NOT_FOUND));

    return createRoomResponse(room);
  }

  /**
   * 객실 수정
   */
  @Transactional
  public RoomResponse updateRoom(
      Long hostId,
      RoomUpdateRequest request,
      MultipartFile newThumbnail
  ) {
    Room room = getAuthorizedRoom(hostId, request.roomId());
    String newThumbnailUrl = null;

    if (newThumbnail != null && !newThumbnail.isEmpty()) {
      newThumbnailUrl = imageService.storeImage(newThumbnail);
      imageService.deleteImageAsync(room.getImageUrl());
    }

    Room updatedRoom = room.updateRoom(
        request,
        newThumbnailUrl != null ? newThumbnailUrl : room.getImageUrl()
    );
    List<RoomFacility> updatedFacilities = updateFacilities(request.facilityTypes(), room);
    List<RoomPetFacility> updatedPetFacilities = updatePetFacilities(
        request.petFacilityTypes(), room);
    List<Hashtag> updatedHashtags = updateHashtags(request.hashtagTypes(), room);

    searchService.save(room.getAccommodation(), updatedRoom); // Elasticsearch 색인 업데이트
    searchService.updateAccommodationDocument(room.getAccommodation());

    return RoomResponse.of(updatedRoom, updatedFacilities, updatedPetFacilities, updatedHashtags);
  }

  /**
   * 객실 삭제
   */
  @Transactional
  public void deleteRoom(Long hostId, Long roomId) {
    Room room = getAuthorizedRoom(hostId, roomId);

    validateExistsReservation(roomId);
    reservationSlotRepository.deleteAllByRoomId(roomId);
    reservationRepository.deleteAllByRoomId(roomId);

    hashtagRepository.deleteAllByRoomId(roomId);
    roomPetFacilityRepository.deleteAllByRoomId(roomId);
    roomFacilityRepository.deleteAllByRoomId(roomId);
    roomRepository.delete(room);

    searchService.delete(room.getAccommodation().getId(), roomId); // Elasticsearch 색인 삭제
    searchService.updateAccommodationDocument(room.getAccommodation());
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
      List<RoomPetFacilityType> newPetFacilityTypes,
      Room room
  ) {
    roomPetFacilityRepository.deleteAllByRoomId(room.getId());
    return saveRoomPetFacilities(newPetFacilityTypes, room);
  }

  private List<RoomPetFacility> saveRoomPetFacilities(
      List<RoomPetFacilityType> petFacilityTypes,
      Room room
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
        .orElseThrow(() -> new MeongnyangerangException(ROOM_NOT_FOUND));
  }

  private void validateRoomAuthorization(Accommodation accommodation, Room room) {
    if (!accommodation.getId().equals(room.getAccommodation().getId())) {
      throw new MeongnyangerangException(ErrorCode.INVALID_AUTHORIZED);
    }
  }

  private void validateExistsReservation(Long roomId) {
    if (reservationRepository.existsByRoom_IdAndStatus(roomId, ReservationStatus.RESERVED)) {
      throw new MeongnyangerangException(ErrorCode.EXISTS_RESERVATION);
    }
  }

  private void validateCreatableCount(Long accommodationId) {
    long roomCount = roomRepository.countByAccommodationId(accommodationId);
    if (roomCount >= ROOM_CREATION_LIMIT_COUNT) {
      throw new MeongnyangerangException(ErrorCode.ROOM_COUNT_LIMIT_EXCEEDED);
    }
  }
}
