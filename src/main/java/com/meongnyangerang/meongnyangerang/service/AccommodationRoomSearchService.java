package com.meongnyangerang.meongnyangerang.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.meongnyangerang.meongnyangerang.component.AccommodationRoomMapper;
import com.meongnyangerang.meongnyangerang.domain.AccommodationRoomDocument;
import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationDocument;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AllowPet;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacility;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacility;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.room.facility.Hashtag;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomFacility;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacility;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationPetFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AllowPetRepository;
import com.meongnyangerang.meongnyangerang.repository.room.HashtagRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomPetFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomRepository;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Elasticsearch 색인 저장 및 삭제를 담당하는 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class AccommodationRoomSearchService {

  private final AccommodationRoomMapper mapper;
  private final ElasticsearchClient elasticsearchClient;
  private final RoomFacilityRepository roomFacilityRepository;
  private final RoomPetFacilityRepository roomPetFacilityRepository;
  private final HashtagRepository hashtagRepository;
  private final AccommodationFacilityRepository accommodationFacilityRepository;
  private final AccommodationPetFacilityRepository accommodationPetFacilityRepository;
  private final AllowPetRepository allowPetRepository;
  private final RoomRepository roomRepository;

  /**
   * 객실 등록 또는 숙소/객실 수정 시 색인 저장
   */
  public void save(Accommodation accommodation, Room room) {
    Long accommodationId = accommodation.getId();
    Long roomId = room.getId();

    List<AccommodationFacility> accFacilities = accommodationFacilityRepository.findAllByAccommodationId(
        accommodationId);
    List<AccommodationPetFacility> accPetFacilities = accommodationPetFacilityRepository.findAllByAccommodationId(
        accommodationId);
    List<AllowPet> allowPets = allowPetRepository.findAllByAccommodationId(accommodationId);

    List<RoomFacility> roomFacilities = roomFacilityRepository.findAllByRoomId(roomId);
    List<RoomPetFacility> roomPetFacilities = roomPetFacilityRepository.findAllByRoomId(roomId);
    List<Hashtag> hashtags = hashtagRepository.findAllByRoomId(roomId);

    AccommodationRoomDocument doc = mapper.toDocument(
        accommodation, room,
        accFacilities, accPetFacilities,
        roomFacilities, roomPetFacilities,
        hashtags, allowPets
    );

    try {
      IndexResponse response = elasticsearchClient.index(i -> i
          .index("accommodation_room")
          .id(doc.getId())
          .document(doc)
      );
      log.info("[색인 저장] 숙소: {}, 객실: {}, result: {}", accommodation.getId(), room.getId(),
          response.result());

    } catch (IOException e) {
      log.error("Elasticsearch 색인 실패: 숙소 {}, 객실 {}, 에러: {}", accommodation.getId(), room.getId(),
          e.getMessage());
      throw new MeongnyangerangException(ErrorCode.ELASTICSEARCH_SAVE_FAILED);
    }
  }

  /**
   * 객실 삭제 시 색인 삭제
   */
  public void delete(Long accommodationId, Long roomId) {
    String id = accommodationId + "_" + roomId;
    try {
      DeleteResponse response = elasticsearchClient.delete(d -> d
          .index("accommodation_room")
          .id(id)
      );
      log.info("[색인 삭제] 숙소: {}, 객실: {}, result: {}", accommodationId, roomId, response.result());
    } catch (IOException e) {
      log.error("Elasticsearch 삭제 실패: 숙소 {}, 객실 {}, 에러: {}", accommodationId, roomId,
          e.getMessage());
    }
  }

  /**
   * 숙소 수정 시 연결된 객실 모두 색인 재저장
   */
  public void updateAllRooms(Accommodation accommodation, List<Room> rooms) {

    if (rooms == null || rooms.isEmpty()) {
      log.info("[색인 갱신] 숙소 ID: {} - 연결된 객실이 없어 색인 생략", accommodation.getId());
      return;
    }
    for (Room room : rooms) {
      save(accommodation, room);
    }
  }

  // 첫 객실 등록 시, 색인하여 accommodations 인덱스에 문서를 저장
  public void indexAccommodationDocument(Accommodation accommodation, Room room) {
    Set<String> allowedPetTypes = getAllowedPetTypes(accommodation);

    Set<String> accommodationPetFacilities = getAccommodationPetFacilities(accommodation);

    Set<String> roomPetFacilities = getRoomPetFacilities(room);

    AccommodationDocument document = toDocument(
        accommodation, room, accommodationPetFacilities, allowedPetTypes, roomPetFacilities);

    try {
      elasticsearchClient.index(index -> index.index("accommodations")
          .id(document.getId().toString())
          .document(document));
    } catch (IOException e) {
      throw new MeongnyangerangException(ErrorCode.ELASTICSEARCH_SAVE_FAILED);
    }
  }

  // 추가 객실 등록 또는 숙소/객실 수정, 객실 삭제 시, 기존 문서에 업데이트
  public void updateAccommodationDocument(Accommodation accommodation) {
    List<Room> rooms = roomRepository.findAllByAccommodationId(accommodation.getId());
    if (rooms.isEmpty()) return;

    Set<String> updatedAccommodationPetFacilities = getAccommodationPetFacilities(accommodation);
    Set<String> updatedRoomPetFacilities = collectRoomPetFacilities(rooms);
    long minPrice = calculateMinRoomPrice(rooms);

    Map<String, Object> doc = Map.of(
        "name", accommodation.getName(),
        "thumbnailUrl", accommodation.getThumbnailUrl(),
        "price", minPrice,
        "accommodationPetFacilities", updatedAccommodationPetFacilities,
        "roomPetFacilities", updatedRoomPetFacilities,
        "allowedPetTypes", getAllowedPetTypes(accommodation)
    );

    updateFields(accommodation.getId(), doc);
  }

  // 리뷰 등록/수정/삭제 시 기존 문서에 업데이트
  public void updateAccommodationTotalRating(Accommodation accommodation, Double totalRating) {
    Map<String, Object> doc = Map.of("totalRating", totalRating);
    updateFields(accommodation.getId(), doc);
  }

  private void updateFields(Long accommodationId, Map<String, Object> fields) {
    try {
      elasticsearchClient.update(
          u -> u.index("accommodations")
              .id(accommodationId.toString())
              .doc(fields),
          AccommodationDocument.class
      );
    } catch (IOException e) {
      throw new MeongnyangerangException(ErrorCode.DOCUMENT_UPDATE_FAILED);
    }
  }

  private Set<String> collectRoomPetFacilities(List<Room> rooms) {
    return rooms.stream()
        .flatMap(r -> roomPetFacilityRepository.findAllByRoomId(r.getId()).stream())
        .map(f -> f.getType().name())
        .collect(Collectors.toSet());
  }

  private long calculateMinRoomPrice(List<Room> rooms) {
    return rooms.stream()
        .map(Room::getPrice)
        .min(Long::compareTo).orElse(0L);
  }

  private AccommodationDocument toDocument(Accommodation accommodation,
      Room room, Set<String> accommodationPetFacilities, Set<String> allowedPetTypes,
      Set<String> roomPetFacilities) {
    return AccommodationDocument.builder()
        .id(accommodation.getId())
        .name(accommodation.getName())
        .thumbnailUrl(accommodation.getThumbnailUrl())
        .price(room.getPrice())
        .totalRating(accommodation.getTotalRating())
        .accommodationPetFacilities(accommodationPetFacilities)
        .allowedPetTypes(allowedPetTypes)
        .roomPetFacilities(roomPetFacilities)
        .build();
  }

  private Set<String> getRoomPetFacilities(Room room) {
    return roomPetFacilityRepository.findAllByRoomId(room.getId())
        .stream().map(RoomPetFacility::getType).map(Enum::name).collect(Collectors.toSet());
  }

  private Set<String> getAccommodationPetFacilities(Accommodation accommodation) {
    return accommodationPetFacilityRepository.findAllByAccommodationId(
            accommodation.getId()).stream().map(AccommodationPetFacility::getType).map(Enum::name)
        .collect(Collectors.toSet());
  }

  private Set<String> getAllowedPetTypes(Accommodation accommodation) {
    return allowPetRepository.findAllByAccommodationId(
            accommodation.getId()).stream().map(AllowPet::getPetType).map(Enum::name)
        .collect(Collectors.toSet());
  }
}