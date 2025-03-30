package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacilityType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationType;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.room.facility.HashtagType;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomFacilityType;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacilityType;
import com.meongnyangerang.meongnyangerang.dto.room.RoomCreateRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.RoomRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.time.LocalTime;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

  @Mock
  private RoomRepository roomRepository;

  @Mock
  private AccommodationRepository accommodationRepository;

  @Mock
  private ImageService imageService;

  @InjectMocks
  private RoomService roomService;

  private RoomCreateRequest roomCreateRequest;
  private Accommodation accommodation;
  private MultipartFile image;
  private String imageUrl;

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

    // 테스트용 숙소 설정
    accommodation = Accommodation.builder()
        .id(1L)
        .name("테스트 숙소")
        .type(AccommodationType.PENSION)
        .build();

    // 테스트용 요청 객체 설정
    List<HashtagType> hashtagTypes = Arrays.asList(HashtagType.FAMILY_TRIP, HashtagType.SPA);
    List<RoomFacilityType> facilityTypes = List.of(RoomFacilityType.STYLER);
    List<RoomPetFacilityType> petFacilityTypes =
        Arrays.asList(CAT_WHEEL, CAT_TOWER, DRY_ROOM, BED, TOY);

    roomCreateRequest = new RoomCreateRequest(
        accommodation.getId(),
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
  }

  @Test
  @DisplayName("객실 생성 성공")
    void createRoom_Success() {
    // given
    Long accommodationId = accommodation.getId();

    when(accommodationRepository.findById(accommodationId)).thenReturn(Optional.of(accommodation));
    when(imageService.storeImage(image)).thenReturn(imageUrl);

    // when
    roomService.createRoom(roomCreateRequest, image);

    // then
    verify(accommodationRepository, times(1)).findById(1L);
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
    when(accommodationRepository.findById(accommodation.getId())).thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(() -> roomService.createRoom(roomCreateRequest, image))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.ACCOMMODATION_NOT_FOUND);

    verify(accommodationRepository).findById(1L);
  }
}