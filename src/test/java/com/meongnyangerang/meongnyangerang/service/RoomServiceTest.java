package com.meongnyangerang.meongnyangerang.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationType;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.room.facility.Hashtag;
import com.meongnyangerang.meongnyangerang.domain.room.facility.HashtagType;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomFacility;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomFacilityType;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacility;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacilityType;
import com.meongnyangerang.meongnyangerang.dto.room.RoomCreateRequest;
import com.meongnyangerang.meongnyangerang.dto.room.RoomResponse;
import com.meongnyangerang.meongnyangerang.dto.room.RoomSummaryResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
  private ReservationRepository reservationRepository;

  @Mock
  private ReservationSlotRepository reservationSlotRepository;

  @Mock
  private ImageService imageService;

  @Mock
  private AccommodationRoomSearchService searchService;

  @InjectMocks
  private RoomService roomService;

  private RoomCreateRequest roomCreateRequest;
  private RoomUpdateRequest roomUpdateRequest;
  private Host host;
  private Accommodation accommodation;
  private Room room;
  private MultipartFile image;
  private String imageUrl;
  private List<Room> rooms;
  private List<RoomFacility> facilities;
  private List<RoomPetFacility> petFacilities;
  private List<Hashtag> hashtags;
  List<RoomFacility> updateFacilities;
  List<RoomPetFacility> updatePetFacilities;
  List<Hashtag> updateHashtags;

  @BeforeEach
  void setUp() {
    image = new MockMultipartFile(
        "image",
        imageUrl,
        "image/jpeg",
        "test image content".getBytes()
    );

    imageUrl = "https://example.com/images/test-image.jpg";

    host = Host.builder()
        .id(1L)
        .build();

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

    List<RoomFacilityType> facilityTypes = List.of(RoomFacilityType.STYLER);
    List<RoomPetFacilityType> petFacilityTypes =
        Arrays.asList(RoomPetFacilityType.CAT_WHEEL, RoomPetFacilityType.CAT_TOWER,
            RoomPetFacilityType.BED, RoomPetFacilityType.TOY);
    List<HashtagType> hashtagTypes = Arrays.asList(HashtagType.FAMILY_TRIP, HashtagType.SPA);

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
        facilityTypes,
        petFacilityTypes,
        hashtagTypes
    );

    rooms = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      Room room = Room.builder()
          .id((long) i)
          .name("객실 " + i)
          .accommodation(accommodation)
          .createdAt(LocalDateTime.of(
              LocalDate.of(2025, 3, 10 + i),
              LocalTime.of(9, 0, 0)))
          .build();

      rooms.add(room);
    }

    roomUpdateRequest = new RoomUpdateRequest(
        room.getId(),
        "디럭스 더블룸",
        "넓고 쾌적한 객실입니다.",
        2,
        4,
        1,
        2,
        120000L,
        30000L,
        20000L,
        10000L,
        LocalTime.of(15, 0),
        LocalTime.of(11, 0),
        facilityTypes,
        petFacilityTypes,
        hashtagTypes
    );

    updateFacilities = Collections.singletonList(
        RoomFacility.builder()
            .id(1L)
            .room(room)
            .type(RoomFacilityType.STYLER)
            .build()
    );

    updatePetFacilities = Arrays.asList(
        RoomPetFacility.builder()
            .id(1L)
            .room(room)
            .type(RoomPetFacilityType.CAT_WHEEL)
            .build(),
        RoomPetFacility.builder()
            .id(2L)
            .room(room)
            .type(RoomPetFacilityType.CAT_TOWER)
            .build(),
        RoomPetFacility.builder()
            .id(4L)
            .room(room)
            .type(RoomPetFacilityType.BED)
            .build(),
        RoomPetFacility.builder()
            .id(4L)
            .room(room)
            .type(RoomPetFacilityType.TOY)
            .build()
    );

    updateHashtags = Arrays.asList(
        Hashtag.builder()
            .id(1L)
            .room(room)
            .type(HashtagType.FAMILY_TRIP)
            .build(),
        Hashtag.builder()
            .id(2L)
            .room(room)
            .type(HashtagType.SPA)
            .build()
    );
  }

  @Test
  @DisplayName("객실 생성 성공")
  void createRoom_Success() {
    // given
    Long hostId = host.getId();
    Long accommodationId = accommodation.getId();

    when(accommodationRepository.findByHostId(hostId)).thenReturn(Optional.of(accommodation));
    when(roomRepository.countByAccommodationId(accommodationId)).thenReturn(10L);
    when(imageService.storeImage(image)).thenReturn(imageUrl);

    // when
    roomService.createRoom(hostId, roomCreateRequest, image);

    // then
    verify(accommodationRepository, times(1)).findByHostId(hostId);
    verify(roomRepository, times(1))
        .countByAccommodationId(accommodationId);
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
  @DisplayName("객실 생성 실패 - 숙소 없음")
  void createRoom_AccommodationNotFound_throwsException() {
    // given
    when(accommodationRepository.findByHostId(host.getId())).thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(() -> roomService.createRoom(host.getId(), roomCreateRequest, image))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.ACCOMMODATION_NOT_FOUND);

    verify(accommodationRepository).findByHostId(host.getId());
  }

  @Test
  @DisplayName("객실 생성 실패 - 숙소 개수 제한")
  void createRoom_RoomCountLimitExceeded_throwException() {
    // given
    when(accommodationRepository.findByHostId(host.getId())).thenReturn(Optional.of(accommodation));
    when(roomRepository.countByAccommodationId(accommodation.getId())).thenReturn(20L);

    // when
    // then
    assertThatThrownBy(() -> roomService.createRoom(host.getId(), roomCreateRequest, image))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.ROOM_COUNT_LIMIT_EXCEEDED);

    verify(accommodationRepository).findByHostId(host.getId());
    verify(roomRepository).countByAccommodationId(accommodation.getId());
  }

  @Test
  @DisplayName("객실 목록 조회 성공")
  void getRoomList_Success() {
    // given
    when(accommodationRepository.findByHostId(host.getId())).thenReturn(Optional.of(accommodation));
    when(roomRepository.findAllByAccommodationId(accommodation.getId())).thenReturn(rooms);

    // when
    List<RoomSummaryResponse> response = roomService.getRoomList(host.getId());

    // then
    assertThat(response).hasSize(5);

    // 모든 객실 포함되었는지 검증
    List<Long> returnedIds = response.stream()
        .map(RoomSummaryResponse::roomId)
        .toList();
    assertThat(returnedIds).containsExactly(1L, 2L, 3L, 4L, 5L);

    // 첫 번째 객실 검증
    RoomSummaryResponse firstRoom = response.get(0);
    assertThat(firstRoom.roomId()).isEqualTo(1L);
    assertThat(firstRoom.name()).isEqualTo("객실 1");

    // 두 번째 객실 검증
    RoomSummaryResponse secondRoom = response.get(1);
    assertThat(secondRoom.roomId()).isEqualTo(2L);
    assertThat(secondRoom.name()).isEqualTo("객실 2");

    // 세 번째 객실 검증
    RoomSummaryResponse thirdRoom = response.get(2);
    assertThat(thirdRoom.roomId()).isEqualTo(3L);
    assertThat(thirdRoom.name()).isEqualTo("객실 3");

    // 네 번째 객실 검증
    RoomSummaryResponse fourthRoom = response.get(3);
    assertThat(fourthRoom.roomId()).isEqualTo(4L);
    assertThat(fourthRoom.name()).isEqualTo("객실 4");

    // 다섯 번째 객실 검증
    RoomSummaryResponse fifthRoom = response.get(4);
    assertThat(fifthRoom.roomId()).isEqualTo(5L);
    assertThat(fifthRoom.name()).isEqualTo("객실 5");
  }

  @Test
  @DisplayName("객실 목록 조회 실패 - 숙소를 찾을 수 없음")
  void getRoomList_AccommodationNotFound_ThrowsException() {
    // given
    when(accommodationRepository.findByHostId(host.getId())).thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(() -> roomService.getRoomList(host.getId()))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.ACCOMMODATION_NOT_FOUND);
  }

  @Test
  @DisplayName("객실 상세 조회 성공(호스트 전용)")
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
    assertThat(response.thumbnailUrl()).isEqualTo(imageUrl);

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
  @DisplayName("객실 상세 조회(호스트 전용) - 객실이 존재하지 않을 때 예외 발생")
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
  @DisplayName("객실 상세 조회(호스트 전용) - 권한이 없을 때 예외 발생")
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

  @Test
  @DisplayName("객실 상세 조회(모든 사용자) - 성공")
  void getRoomDetail_Success() {
    // given
    Long roomId = 1L;
    Room room = Room.builder()
        .id(roomId)
        .name("펫룸")
        .description("반려견 동반 가능")
        .standardPeopleCount(2)
        .maxPeopleCount(4)
        .standardPetCount(1)
        .maxPetCount(2)
        .price(100_000L)
        .extraFee(10_000L)
        .extraPeopleFee(5_000L)
        .extraPetFee(5_000L)
        .checkInTime(LocalTime.of(15, 0))
        .checkOutTime(LocalTime.of(11, 0))
        .imageUrl("https://image.com/room.jpg")
        .build();

    List<RoomFacility> facilities = List.of(
        RoomFacility.builder().type(RoomFacilityType.TV).build(),
        RoomFacility.builder().type(RoomFacilityType.AIR_CONDITIONER).build()
    );

    List<RoomPetFacility> petFacilities = List.of(
        RoomPetFacility.builder().type(RoomPetFacilityType.BED).build()
    );

    List<Hashtag> hashtags = List.of(
        Hashtag.builder().type(HashtagType.SPA).build()
    );

    given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
    given(facilityRepository.findAllByRoomId(roomId)).willReturn(facilities);
    given(petFacilityRepository.findAllByRoomId(roomId)).willReturn(petFacilities);
    given(hashtagRepository.findAllByRoomId(roomId)).willReturn(hashtags);

    // when
    RoomResponse result = roomService.getRoomDetail(roomId);

    // then
    assertThat(result.name()).isEqualTo("펫룸");
    assertThat(result.standardPeopleCount()).isEqualTo(2);
    assertThat(result.facilityTypes()).containsExactly("TV", "에어컨");
    assertThat(result.petFacilityTypes()).contains("침대");
    assertThat(result.hashtagTypes()).contains("스파");
  }

  @Test
  @DisplayName("객실 상세 조회(모든 사용자) - 실패(존재하지 않는 roomId)")
  void getRoomDetail_Fail_NotFound() {
    // given
    Long invalidRoomId = 999L;
    given(roomRepository.findById(invalidRoomId)).willReturn(Optional.empty());

    // when
    Throwable throwable = catchThrowable(() -> roomService.getRoomDetail(invalidRoomId));

    // then
    assertThat(throwable).isInstanceOf(MeongnyangerangException.class);
    MeongnyangerangException ex = (MeongnyangerangException) throwable;
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ROOM_NOT_FOUND);
  }

  @Test
  @DisplayName("객실 수정 성공")
  void updateRoom_Success() {
    // given
    MultipartFile newImage = new MockMultipartFile(
        "image",
        imageUrl,
        "image/jpeg",
        "test image content".getBytes()
    );

    when(accommodationRepository.findByHostId(host.getId()))
        .thenReturn(Optional.ofNullable(accommodation));
    when(roomRepository.findById(room.getId())).thenReturn(Optional.ofNullable(room));

    // when
    RoomResponse response = roomService.updateRoom(host.getId(), roomUpdateRequest, newImage);

    // then
    assertThat(response.roomId()).isEqualTo(room.getId());
    assertThat(response.name()).isEqualTo(roomUpdateRequest.name());
    assertThat(response.description()).isEqualTo(roomUpdateRequest.description());
    assertThat(response.standardPeopleCount()).isEqualTo(roomUpdateRequest.standardPeopleCount());
    assertThat(response.maxPeopleCount()).isEqualTo(roomUpdateRequest.maxPeopleCount());
    assertThat(response.standardPetCount()).isEqualTo(roomUpdateRequest.standardPetCount());
    assertThat(response.maxPetCount()).isEqualTo(roomUpdateRequest.maxPetCount());
    assertThat(response.price()).isEqualTo(roomUpdateRequest.price());
    assertThat(response.extraPeopleFee()).isEqualTo(roomUpdateRequest.extraPeopleFee());
    assertThat(response.extraPetFee()).isEqualTo(roomUpdateRequest.extraPetFee());
    assertThat(response.extraFee()).isEqualTo(roomUpdateRequest.extraFee());
    assertThat(response.checkInTime()).isEqualTo(roomUpdateRequest.checkInTime());
    assertThat(response.checkOutTime()).isEqualTo(roomUpdateRequest.checkOutTime());
    assertThat(response.thumbnailUrl()).isEqualTo(imageUrl);
  }

  @Test
  @DisplayName("객실 수정 - 숙소가 존재하지 않는 경우 예외 발생")
  void updateRoom_AccommodationNotFound_ThrowsExceptions() {
    // given
    when(accommodationRepository.findByHostId(accommodation.getId()))
        .thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(() -> roomService.updateRoom(host.getId(), roomUpdateRequest, image))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.ACCOMMODATION_NOT_FOUND);
  }

  @Test
  @DisplayName("객실 수정 - 객실이 존재하지 않는 경우")
  void updateRoom_RoomNotFound() {
    // given
    when(accommodationRepository.findByHostId(accommodation.getId()))
        .thenReturn(Optional.of(accommodation));
    when(roomRepository.findById(room.getId())).thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(() -> roomService.updateRoom(host.getId(), roomUpdateRequest, image))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.ROOM_NOT_FOUND);
  }

  @Test
  @DisplayName("객실 수정 - 권한이 없는 경우")
  void updateRoom_InvalidAuthorized_ThrowsException() {
    // given
    Room otherRoom = Room.builder()
        .id(200L)
        .accommodation(new Accommodation())
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

    when(accommodationRepository.findByHostId(accommodation.getId()))
        .thenReturn(Optional.of(accommodation));
    when(roomRepository.findById(room.getId())).thenReturn(Optional.of(otherRoom));
    when(roomRepository.findById(room.getId())).thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(() -> roomService.updateRoom(host.getId(), roomUpdateRequest, image))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.ROOM_NOT_FOUND);
  }

  @Test
  @DisplayName("객실 삭제 성공")
  void deleteRoom_Success() {
    // given
    Long hostId = host.getId();
    Long roomId = room.getId();

    when(accommodationRepository.findByHostId(hostId)).thenReturn(Optional.of(accommodation));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(reservationRepository.existsByRoom_IdAndStatus(roomId,
        ReservationStatus.RESERVED)).thenReturn(false);

    // when
    roomService.deleteRoom(host.getId(), roomId);

    // then
    verify(accommodationRepository, times(1)).findByHostId(hostId);
    verify(roomRepository, times(1)).findById(roomId);
    verify(reservationRepository, times(1))
        .existsByRoom_IdAndStatus(roomId, ReservationStatus.RESERVED);
    verify(reservationSlotRepository, times(1)).deleteAllByRoomId(roomId);
    verify(reservationRepository, times(1)).deleteAllByRoomId(roomId);
    verify(hashtagRepository, times(1)).deleteAllByRoomId(roomId);
    verify(petFacilityRepository, times(1)).deleteAllByRoomId(roomId);
    verify(facilityRepository, times(1)).deleteAllByRoomId(roomId);
    verify(roomRepository, times(1)).delete(room);
  }

  @Test
  @DisplayName("객실 삭제 실패 - 숙소 없음")
  void deleteRoom_AccommodationNotFound_ThrowsException() {
    // given
    Long hostId = host.getId();
    Long roomId = room.getId();

    when(accommodationRepository.findByHostId(hostId)).thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(() -> roomService.deleteRoom(hostId, roomId))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.ACCOMMODATION_NOT_FOUND);

    verify(accommodationRepository, times(1)).findByHostId(hostId);
    verify(roomRepository, never()).findById(roomId);
    verify(reservationRepository, never()).existsByRoom_IdAndStatus(roomId,
        ReservationStatus.RESERVED);
    verify(reservationSlotRepository, never()).deleteAllByRoomId(roomId);
    verify(reservationRepository, never()).deleteAllByRoomId(roomId);
    verify(hashtagRepository, never()).deleteAllByRoomId(roomId);
    verify(petFacilityRepository, never()).deleteAllByRoomId(roomId);
    verify(facilityRepository, never()).deleteAllByRoomId(roomId);
    verify(roomRepository, never()).delete(room);
  }

  @Test
  @DisplayName("객실 삭제 실패 - 객실 없음")
  void deleteRoom_RoomNotFound_ThrowsException() {
    // given
    Long hostId = host.getId();
    Long roomId = room.getId();

    when(accommodationRepository.findByHostId(hostId)).thenReturn(Optional.of(accommodation));
    when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(() -> roomService.deleteRoom(hostId, roomId))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.ROOM_NOT_FOUND);

    verify(accommodationRepository, times(1)).findByHostId(hostId);
    verify(roomRepository, times(1)).findById(roomId);
    verify(reservationRepository, never())
        .existsByRoom_IdAndStatus(roomId, ReservationStatus.RESERVED);
    verify(reservationSlotRepository, never()).deleteAllByRoomId(roomId);
    verify(reservationRepository, never()).deleteAllByRoomId(roomId);
    verify(hashtagRepository, never()).deleteAllByRoomId(roomId);
    verify(petFacilityRepository, never()).deleteAllByRoomId(roomId);
    verify(facilityRepository, never()).deleteAllByRoomId(roomId);
    verify(roomRepository, never()).delete(room);
  }

  @Test
  @DisplayName("객실 삭제 실패 - 권한 없음")
  void deleteRoom_InvalidAuthorized_ThrowsException() {
    // given
    Long hostId = host.getId();
    Long roomId = room.getId();

    Accommodation otherAccommodation = Accommodation.builder().id(20L).build();

    when(accommodationRepository.findByHostId(hostId)).thenReturn(Optional.of(otherAccommodation));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

    // when
    // then
    assertThatThrownBy(() -> roomService.deleteRoom(hostId, roomId))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.INVALID_AUTHORIZED);

    verify(accommodationRepository, times(1)).findByHostId(hostId);
    verify(roomRepository, times(1)).findById(roomId);
    verify(reservationRepository, never())
        .existsByRoom_IdAndStatus(roomId, ReservationStatus.RESERVED);
    verify(reservationSlotRepository, never()).deleteAllByRoomId(roomId);
    verify(reservationRepository, never()).deleteAllByRoomId(roomId);
    verify(hashtagRepository, never()).deleteAllByRoomId(roomId);
    verify(petFacilityRepository, never()).deleteAllByRoomId(roomId);
    verify(facilityRepository, never()).deleteAllByRoomId(roomId);
    verify(roomRepository, never()).delete(room);
  }

  @Test
  @DisplayName("객실 삭제 실패 - 예약 존재")
  void deleteRoom_ExistsReservation_ThrowsException() {
    // given
    Long hostId = host.getId();
    Long roomId = room.getId();

    when(accommodationRepository.findByHostId(hostId)).thenReturn(Optional.of(accommodation));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(reservationRepository.existsByRoom_IdAndStatus(roomId, ReservationStatus.RESERVED))
        .thenReturn(true);

    // when
    // then
    assertThatThrownBy(() -> roomService.deleteRoom(hostId, roomId))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.EXISTS_RESERVATION);

    verify(accommodationRepository, times(1)).findByHostId(hostId);
    verify(roomRepository, times(1)).findById(roomId);
    verify(reservationRepository, times(1))
        .existsByRoom_IdAndStatus(roomId, ReservationStatus.RESERVED);
    verify(reservationSlotRepository, never()).deleteAllByRoomId(roomId);
    verify(reservationRepository, never()).deleteAllByRoomId(roomId);
    verify(hashtagRepository, never()).deleteAllByRoomId(roomId);
    verify(petFacilityRepository, never()).deleteAllByRoomId(roomId);
    verify(facilityRepository, never()).deleteAllByRoomId(roomId);
    verify(roomRepository, never()).delete(room);
  }
}