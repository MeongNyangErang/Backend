package com.meongnyangerang.meongnyangerang.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationType;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
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
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.room.HashtagRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomPetFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

  @Mock
  private RoomRepository roomRepository;

  @Mock
  private RoomFacilityRepository facilityRepository;

  @Mock
  private RoomPetFacilityRepository petFacilityRepository;

  @Mock
  private HashtagRepository hashtagRepository;

  @Mock
  private AccommodationRepository accommodationRepository;

  @Mock
  private ImageService imageService;

  @InjectMocks
  private RoomService roomService;

  private RoomCreateRequest roomCreateRequest;
  private Host host;
  private Accommodation accommodation;
  private Room room;
  private MultipartFile image;
  private String imageUrl;
  private List<Room> rooms;
  private List<RoomFacility> facilities;
  private List<RoomPetFacility> petFacilities;
  private List<Hashtag> hashtags;

  private final int PAGE_SIZE = 5;

  Pageable pageable = PageRequest.of(
      0, PAGE_SIZE + 1, Sort.by(Sort.Direction.DESC, "id"));

  @BeforeEach
  void setUp() {
    // 테스트용 이미지 설정
    image = new MockMultipartFile(
        "image",
        "test-image.jpg",
        "image/jpeg",
        "test image content".getBytes()
    );

    imageUrl = "https://example.com/images/test-image.jpg";

    host = Host.builder()
        .id(1L)
        .build();

    // 테스트용 숙소 설정
    accommodation = Accommodation.builder()
        .id(1L)
        .host(host)
        .name("테스트 숙소")
        .type(AccommodationType.PENSION)
        .build();

    room = Room.builder()
        .id(200L)
        .accommodation(accommodation)
        .name("디럭스 룸")
        .description("아늑한 객실입니다")
        .standardPeopleCount(2)
        .maxPeopleCount(4)
        .standardPetCount(1)
        .maxPetCount(2)
        .price(100000L)
        .extraPeopleFee(20000L)
        .extraPetFee(15000L)
        .extraFee(5000L)
        .checkInTime(LocalTime.of(15, 0))
        .checkOutTime(LocalTime.of(11, 0))
        .imageUrl(imageUrl)
        .build();

    // 테스트용 요청 객체 설정
    List<HashtagType> hashtagTypes = Arrays.asList(HashtagType.FAMILY_TRIP, HashtagType.SPA);
    List<RoomFacilityType> facilityTypes = List.of(RoomFacilityType.STYLER);
    List<RoomPetFacilityType> petFacilityTypes =
        Arrays.asList(RoomPetFacilityType.CAT_WHEEL, RoomPetFacilityType.CAT_TOWER,
            RoomPetFacilityType.DRY_ROOM, RoomPetFacilityType.BED, RoomPetFacilityType.TOY);

    roomCreateRequest = new RoomCreateRequest(
        "test-room-name",
        "객실 설명",
        6,
        12,
        3,
        6,
        129800L,
        20000L,
        null,
        15000L,
        LocalTime.of(15, 30),
        LocalTime.of(10, 30),
        hashtagTypes,
        facilityTypes,
        petFacilityTypes
    );

    LocalDateTime now = LocalDateTime.now();
    rooms = new ArrayList<>();

    for (int i = 1; i <= 10; i++) {
      Room room = Room.builder()
          .id((long) i)
          .name("객실 " + i)
          .accommodation(accommodation)
          .createdAt(now.minusDays(i))
          .build();

      rooms.add(room);
    }

    // 시설 목록 생성
    facilities = Arrays.asList(
        RoomFacility.builder()
            .id(1L)
            .room(room)
            .type(RoomFacilityType.TV)
            .build(),
        RoomFacility.builder()
            .id(2L)
            .room(room)
            .type(RoomFacilityType.WIFI)
            .build()
    );

    // 반려동물 시설 목록 생성
    petFacilities = Arrays.asList(
        RoomPetFacility.builder()
            .id(1L)
            .room(room)
            .type(RoomPetFacilityType.BED)
            .build(),
        RoomPetFacility.builder()
            .id(2L)
            .room(room)
            .type(RoomPetFacilityType.FOOD_BOWL)
            .build()
    );

    // 해시태그 목록 생성
    hashtags = Arrays.asList(
        Hashtag.builder()
            .id(1L)
            .room(room)
            .type(HashtagType.COZY)
            .build(),
        Hashtag.builder()
            .id(2L)
            .room(room)
            .type(HashtagType.MODERN)
            .build()
    );
  }

  @Test
  @DisplayName("객실 생성 성공")
  void createRoom_Success() {
    // given
    when(accommodationRepository.findByHostId(host.getId())).thenReturn(Optional.of(accommodation));
    when(imageService.storeImage(image)).thenReturn(imageUrl);

    // when
    roomService.createRoom(host.getId(), roomCreateRequest, image);

    // then
    verify(accommodationRepository, times(1)).findByHostId(1L);
    verify(imageService, times(1)).storeImage(image);

    ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
    verify(roomRepository).save(roomCaptor.capture());

    // 캡처된 Room 객체 검증
    Room savedRoom = roomCaptor.getValue();

    assertThat(savedRoom.getAccommodation()).isEqualTo(accommodation);
    assertThat(savedRoom.getName()).isEqualTo(roomCreateRequest.name());
    assertThat(savedRoom.getDescription()).isEqualTo(roomCreateRequest.description());
    assertThat(savedRoom.getStandardPeopleCount())
        .isEqualTo(roomCreateRequest.standardPeopleCount());
    assertThat(savedRoom.getMaxPeopleCount()).isEqualTo(roomCreateRequest.maxPeopleCount());
    assertThat(savedRoom.getStandardPetCount()).isEqualTo(roomCreateRequest.standardPetCount());
    assertThat(savedRoom.getMaxPetCount()).isEqualTo(roomCreateRequest.maxPetCount());
    assertThat(savedRoom.getPrice()).isEqualTo(roomCreateRequest.price());
    assertThat(savedRoom.getExtraPeopleFee()).isEqualTo(roomCreateRequest.extraPeopleFee());
    assertThat(savedRoom.getExtraPetFee()).isEqualTo(roomCreateRequest.extraPetFee());
    assertThat(savedRoom.getExtraFee()).isEqualTo(roomCreateRequest.extraFee());
    assertThat(savedRoom.getCheckInTime()).isEqualTo(roomCreateRequest.checkInTime());
    assertThat(savedRoom.getCheckOutTime()).isEqualTo(roomCreateRequest.checkOutTime());
    assertThat(savedRoom.getImageUrl()).isEqualTo(imageUrl);
  }

  @Test
  @DisplayName("숙소를 찾을 수 없을 때 예외 발생")
  void createRoom_AccommodationNotFound() {
    // given
    when(accommodationRepository.findByHostId(host.getId())).thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(() -> roomService.createRoom(host.getId(), roomCreateRequest, image))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.ACCOMMODATION_NOT_FOUND);

    verify(accommodationRepository).findByHostId(1L);
  }

  @Test
  @DisplayName("객실 목록 조회 성공 - 첫 페이지 조회")
  void getRoomList_FirstPage_Success() {
    // given
    int pageSize = 5;

    when(accommodationRepository.findByHostId(host.getId())).thenReturn(
        Optional.of(accommodation));
    when(roomRepository.findRoomsWithCursor(accommodation.getId(), null, pageable))
        .thenReturn(rooms.subList(0, 6)); // 5개 요청 + 1개 추가

    // when
    RoomListResponse response = roomService.getRoomList(host.getId(), null, pageSize);

    // then
    assertThat(response.content()).hasSize(5);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.nextCursor()).isEqualTo(5L); // 마지막으로 조회된 객실의 ID
  }

  @Test
  @DisplayName("객실 목록 조회 성공 - 다음 페이지")
  void getRoomList_NextPage_Success() {
    // given
    Long cursorId = 6L; // 이전 페이지의 마지막 ID
    int pageSize = 5;

    when(accommodationRepository.findByHostId(host.getId()))
        .thenReturn(Optional.of(accommodation));
    when(roomRepository.findRoomsWithCursor(accommodation.getId(), cursorId, pageable))
        .thenReturn(rooms.subList(5, 10)); // ID가 6 ~ 10인 객실

    // when
    RoomListResponse response = roomService.getRoomList(host.getId(), cursorId, pageSize);

    // then
    assertThat(response.content()).hasSize(5);
    assertThat(response.hasNext()).isFalse(); // 다음 페이지 없음
    assertThat(response.nextCursor()).isNull();
  }

  @Test
  @DisplayName("객실 목록 조회 성공 - 결과가 없는 경우")
  void getRoomList_EmptyResult_Success() {
    // given
    Long cursorId = 1L;
    int pageSize = 5;

    when(accommodationRepository.findByHostId(host.getId()))
        .thenReturn(Optional.of(accommodation));
    when(roomRepository.findRoomsWithCursor(accommodation.getId(), cursorId, pageable))
        .thenReturn(new ArrayList<>()); // 빈 결과

    // when
    RoomListResponse response = roomService.getRoomList(host.getId(), cursorId, pageSize);

    // then
    assertThat(response.content()).isEmpty();
    assertThat(response.hasNext()).isFalse();
    assertThat(response.nextCursor()).isNull();
  }

  @Test
  @DisplayName("객실 목록 조회 실패 - 숙소를 찾을 수 없음")
  void getRoomList_AccommodationNotFound_ThrowsException() {
    // given
    int pageSize = 5;

    when(accommodationRepository.findByHostId(host.getId())).thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(() -> roomService.getRoomList(
        host.getId(), null, pageSize))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.ACCOMMODATION_NOT_FOUND);
  }

  @Test
  @DisplayName("객실 상세 조회 성공")
  void getRoom_Success() {
    // given
    Long hostId = host.getId();
    Long roomId = room.getId();

    when(accommodationRepository.findByHostId(hostId)).thenReturn(Optional.of(accommodation));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(facilityRepository.findAllByRoomId(roomId)).thenReturn(facilities);
    when(petFacilityRepository.findAllByRoomId(roomId)).thenReturn(petFacilities);
    when(hashtagRepository.findAllByRoomId(roomId)).thenReturn(hashtags);

    // when
    RoomResponse response = roomService.getRoom(hostId, roomId);

    // then
    assertThat(response.name()).isEqualTo("디럭스 룸");
    assertThat(response.description()).isEqualTo("아늑한 객실입니다");
    assertThat(response.standardPeopleCount()).isEqualTo(2);
    assertThat(response.maxPeopleCount()).isEqualTo(4);
    assertThat(response.price()).isEqualTo(100000L);
    assertThat(response.imageUrl()).isEqualTo(imageUrl);

    // 시설 목록 검증
    assertThat(response.facilityTypes()).hasSize(2);
    assertThat(response.facilityTypes()).contains(
        RoomFacilityType.TV.getValue(), RoomFacilityType.WIFI.getValue());

    // 반려동물 시설 목록 검증
    assertThat(response.petFacilityTypes()).hasSize(2);
    assertThat(response.petFacilityTypes()).contains(
        RoomPetFacilityType.BED.getValue(), RoomPetFacilityType.FOOD_BOWL.getValue());

    // 해시태그 목록 검증
    assertThat(response.hashtagTypes()).hasSize(2);
    assertThat(response.hashtagTypes()).contains(
        HashtagType.COZY.getValue(), HashtagType.MODERN.getValue());
  }


  @Test
  @DisplayName("객실이 존재하지 않을 때 예외 발생")
  void getRoom_RoomNotFound() {
    // given
    Long hostId = host.getId();
    Long roomId = 999L;

    when(accommodationRepository.findByHostId(hostId)).thenReturn(Optional.of(accommodation));
    when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(() -> roomService.getRoom(hostId, roomId))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.ROOM_NOT_FOUND);
  }

  @Test
  @DisplayName("권한이 없을 때 예외 발생")
  void getRoom_InvalidAuthorized() {
    // given
    Long hostId = host.getId();
    Long roomId = room.getId();

    // 다른 숙소에 속한 객실로 설정
    Accommodation otherAccommodation = Accommodation.builder()
        .id(200L) // 다른 숙소 ID
        .build();

    when(accommodationRepository.findByHostId(hostId)).thenReturn(Optional.of(otherAccommodation));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

    // when
    // then
    assertThatThrownBy(() -> roomService.getRoom(hostId, roomId))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.INVALID_AUTHORIZED);
  }
}