package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.component.AccommodationRoomMapper;
import com.meongnyangerang.meongnyangerang.domain.AccommodationRoomDocument;
import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AllowPet;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacility;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacility;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.room.facility.Hashtag;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomFacility;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacility;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationPetFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AllowPetRepository;
import com.meongnyangerang.meongnyangerang.repository.room.HashtagRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomPetFacilityRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

// Elasticsearch 색인 저장 및 삭제를 담당하는 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class AccommodationRoomSearchService {

  private final AccommodationRoomMapper mapper;
  private final ElasticsearchOperations elasticsearchOperations;
  private final RoomFacilityRepository roomFacilityRepository;
  private final RoomPetFacilityRepository roomPetFacilityRepository;
  private final HashtagRepository hashtagRepository;
  private final AccommodationFacilityRepository accommodationFacilityRepository;
  private final AccommodationPetFacilityRepository accommodationPetFacilityRepository;
  private final AllowPetRepository allowPetRepository;

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

    elasticsearchOperations.save(mapper.toDocument(
        accommodation,
        room,
        accFacilities,
        accPetFacilities,
        roomFacilities,
        roomPetFacilities,
        hashtags,
        allowPets
    ));
    log.info("[색인 저장] 숙소: {}, 객실: {} 색인 완료", accommodationId, roomId);
  }

  /**
   * 객실 삭제 시 색인 삭제
   */
  public void delete(Long accommodationId, Long roomId) {
    String id = accommodationId + "_" + roomId;
    elasticsearchOperations.delete(id, AccommodationRoomDocument.class);
    log.info("[색인 삭제] 숙소: {}, 객실: {} 색인 삭제 완료", accommodationId, roomId);
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
}