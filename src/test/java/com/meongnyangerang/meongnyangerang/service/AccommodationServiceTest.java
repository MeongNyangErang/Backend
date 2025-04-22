package com.meongnyangerang.meongnyangerang.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationImage;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AllowPet;
import com.meongnyangerang.meongnyangerang.domain.accommodation.PetType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacility;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacilityType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacility;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacilityType;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.host.HostStatus;
import com.meongnyangerang.meongnyangerang.domain.review.Review;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationCreateRequest;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationDetailResponse;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationResponse;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationUpdateRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewRepository;
import com.meongnyangerang.meongnyangerang.repository.WishlistRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationImageRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationPetFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AllowPetRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.time.LocalDateTime;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class AccommodationServiceTest {

  @Mock
  private HostRepository hostRepository;

  @Mock
  private AccommodationRepository accommodationRepository;

  @Mock
  private AccommodationFacilityRepository accommodationFacilityRepository;

  @Mock
  private AccommodationPetFacilityRepository accommodationPetFacilityRepository;

  @Mock
  private AllowPetRepository allowPetRepository;

  @Mock
  private AccommodationImageRepository accommodationImageRepository;

  @Mock
  private ImageService imageService;

  @Mock
  private RoomRepository roomRepository;

  @Mock
  private ReviewRepository reviewRepository;

  @Mock
  private WishlistRepository wishlistRepository;

  @Mock
  private AccommodationRoomSearchService searchService;

  @InjectMocks
  private AccommodationService accommodationService;

  private static final String THUMBNAIL_URL = "https://test.com/image/thumbnail-123.jpg";
  private static final String ADDITIONAL_IMAGE_URL1 = "https://test.com/image/image1-456.jpg";
  private static final String ADDITIONAL_IMAGE_URL2 = "https://test.com/image/image2-456.jpg";
  private static final String OLD_THUMBNAIL_URL = "https://test.com/image/thumbnail-123.jpg";

  private static final List<AccommodationFacilityType> FACILITY_TYPES = Arrays
      .asList(AccommodationFacilityType.WIFI, AccommodationFacilityType.PUBLIC_SWIMMING_POOL);
  private static final List<AccommodationPetFacilityType> PET_FACILITY_TYPES = Arrays
      .asList(AccommodationPetFacilityType.PET_FOOD, AccommodationPetFacilityType.EXERCISE_AREA);
  private static final List<PetType> PET_TYPES = Arrays
      .asList(PetType.SMALL_DOG, PetType.MEDIUM_DOG);

  private Host host;
  private AccommodationCreateRequest request;
  private AccommodationUpdateRequest updateRequest;
  private MockMultipartFile thumbnail;
  private List<MultipartFile> additionalImages;

  private Accommodation accommodation;
  private List<AccommodationFacility> facilities;
  private List<AccommodationPetFacility> petFacilities;
  private List<AllowPet> allowPets;
  private List<AccommodationImage> accommodationImages;
  private List<String> deleteImageUrls;

  @BeforeEach
  void setUp() {
    host = Host.builder()
        .id(1L)
        .name("호스트명")
        .email("host@example.com")
        .nickname("호스트닉네임")
        .status(HostStatus.ACTIVE)
        .build();

    accommodation = Accommodation.builder()
        .id(host.getId())
        .host(host)
        .name("숙소명")
        .description("숙소 설명")
        .address("서울시 강남구")
        .detailedAddress("test 아파트 101동 101호")
        .latitude(32.123)
        .longitude(127.123)
        .type(AccommodationType.FULL_VILLA)
        .thumbnailUrl(THUMBNAIL_URL)
        .build();

    request = new AccommodationCreateRequest(
        "test-name",
        AccommodationType.DETACHED_HOUSE,
        "test-address",
        "test-detailedAddress",
        "test-description",
        37.123,
        127.123,
        FACILITY_TYPES,
        PET_FACILITY_TYPES,
        PET_TYPES
    );

    updateRequest = AccommodationUpdateRequest.of(
        "test-name",
        AccommodationType.PENSION,
        "test-address",
        "test-detailedAddress",
        "test-description",
        37.123,
        127.123,
        FACILITY_TYPES,
        PET_FACILITY_TYPES,
        PET_TYPES,
        deleteImageUrls
    );

    facilities = Arrays.asList(AccommodationFacility.builder()
            .id(1L)
            .accommodation(accommodation)
            .type(AccommodationFacilityType.WIFI)
            .build(),
        AccommodationFacility.builder()
            .id(2L)
            .accommodation(accommodation)
            .type(AccommodationFacilityType.PUBLIC_SWIMMING_POOL)
            .build()
    );

    petFacilities = Arrays.asList(AccommodationPetFacility.builder()
            .id(1L)
            .accommodation(accommodation)
            .type(AccommodationPetFacilityType.PET_FOOD)
            .build(),
        AccommodationPetFacility.builder()
            .id(2L)
            .accommodation(accommodation)
            .type(AccommodationPetFacilityType.EXERCISE_AREA)
            .build()
    );

    allowPets = Arrays.asList(AllowPet.builder()
            .id(1L)
            .accommodation(accommodation)
            .petType(PetType.SMALL_DOG)
            .build(),
        AllowPet.builder()
            .id(2L)
            .accommodation(accommodation)
            .petType(PetType.MEDIUM_DOG)
            .build()
    );

    accommodationImages = Arrays.asList(
        AccommodationImage.builder()
            .id(1L)
            .accommodation(accommodation)
            .imageUrl(ADDITIONAL_IMAGE_URL1)
            .build(),
        AccommodationImage.builder()
            .id(2L)
            .accommodation(accommodation)
            .imageUrl(ADDITIONAL_IMAGE_URL2)
            .build()
    );

    thumbnail = new MockMultipartFile(
        "thumbnail",
        "thumbnail.jpg",
        "image/jpg",
        "thumbnail reviewContent".getBytes()
    );

    additionalImages = List.of(
        new MockMultipartFile(
            "image1",
            "image1.jpg",
            "image/jpg",
            "image1 reviewContent".getBytes())
    );

    deleteImageUrls = List.of("test-delete-image1.jpg", "test-delete-image2.jpg");
  }

  @Test
  @DisplayName("숙소 등록 - 성공")
  void createAccommodation_Success() {
    // given
    when(hostRepository.findById(host.getId())).thenReturn(Optional.of(host));
    when(accommodationRepository.existsByHostId(host.getId())).thenReturn(false);

    when(imageService.storeImage(thumbnail)).thenReturn(THUMBNAIL_URL);
    when(imageService.storeImage(additionalImages.get(0))).thenReturn(ADDITIONAL_IMAGE_URL1);

    ArgumentCaptor<Accommodation> accommodationCaptor = ArgumentCaptor.forClass(
        Accommodation.class);
    when(accommodationRepository.save(accommodationCaptor.capture())).thenReturn(accommodation);

    ArgumentCaptor<List<AccommodationFacility>> facilitiesCaptor = ArgumentCaptor.forClass(
        List.class);
    ArgumentCaptor<List<AccommodationPetFacility>> petFacilitiesCaptor = ArgumentCaptor.forClass(
        List.class);
    ArgumentCaptor<List<AllowPet>> allowPetsCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<List<AccommodationImage>> accommodationImageCaptor = ArgumentCaptor.forClass(
        List.class);

    // when
    accommodationService.createAccommodation(host.getId(), request, thumbnail, additionalImages);

    // then
    verify(hostRepository).findById(host.getId());
    verify(accommodationRepository).existsByHostId(host.getId());

    verify(imageService).storeImage(thumbnail);
    verify(imageService).storeImage(additionalImages.get(0));

    verify(accommodationRepository).save(accommodationCaptor.capture());
    Accommodation savedAccommodation = accommodationCaptor.getValue();
    assertEquals(THUMBNAIL_URL, savedAccommodation.getThumbnailUrl());

    verify(accommodationFacilityRepository).saveAll(facilitiesCaptor.capture());
    List<AccommodationFacility> capturedFacilities = facilitiesCaptor.getValue();
    assertEquals(2, capturedFacilities.size());

    verify(accommodationPetFacilityRepository).saveAll(petFacilitiesCaptor.capture());
    List<AccommodationPetFacility> capturedPetFacilities = petFacilitiesCaptor.getValue();
    assertEquals(2, capturedPetFacilities.size());

    verify(allowPetRepository).saveAll(allowPetsCaptor.capture());
    List<AllowPet> capturedAllowPets = allowPetsCaptor.getValue();
    assertEquals(2, capturedAllowPets.size());

    verify(accommodationImageRepository).saveAll(accommodationImageCaptor.capture());
    List<AccommodationImage> capturedImages = accommodationImageCaptor.getValue();
    assertEquals(1, capturedImages.size());
    assertEquals(ADDITIONAL_IMAGE_URL1, capturedImages.get(0).getImageUrl());
  }

  @Test
  @DisplayName("숙소 등록 - 호스트 없음 실패")
  void createAccommodation_HostNotFound_ThrowsException() {
    // given
    when(hostRepository.findById(host.getId())).thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(() -> accommodationService.createAccommodation(
        host.getId(), request, thumbnail, additionalImages))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.NOT_EXISTS_HOST);

    verify(hostRepository).findById(host.getId());
  }

  @Test
  @DisplayName("숙소 등록 - 이미 숙소 있음 실패")
  void createAccommodation_AccommodationAlreadyExists_ThrowsException() {
    // given
    when(hostRepository.findById(host.getId())).thenReturn(Optional.of(host));
    when(accommodationRepository.existsByHostId(host.getId())).thenReturn(true);

    // when
    // then
    assertThatThrownBy(() -> accommodationService.createAccommodation(
        host.getId(), request, thumbnail, additionalImages))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.ACCOMMODATION_ALREADY_EXISTS);

    verify(hostRepository).findById(host.getId());
    verify(accommodationRepository).existsByHostId(host.getId());
  }

  @Test
  @DisplayName("숙소 조회 - 성공")
  void getAccommodation_Success() {
    // given
    Long accommodationId = accommodation.getId();

    when(accommodationRepository.findByHostId(host.getId())).thenReturn(Optional.of(accommodation));
    when(accommodationFacilityRepository.findAllByAccommodationId(accommodationId))
        .thenReturn(facilities);
    when(accommodationPetFacilityRepository.findAllByAccommodationId(accommodationId))
        .thenReturn(petFacilities);
    when(allowPetRepository.findAllByAccommodationId(accommodationId)).thenReturn(allowPets);
    when(accommodationImageRepository.findAllByAccommodationId(accommodationId))
        .thenReturn(accommodationImages);

    // when
    AccommodationResponse response = accommodationService.getAccommodation(host.getId());

    // then
    // 숙소 기본 정보 검증
    assertThat(response.accommodationId()).isEqualTo(accommodationId);
    assertThat(response.name()).isEqualTo(accommodation.getName());
    assertThat(response.type()).isEqualTo(accommodation.getType().getValue());
    assertThat(response.address()).isEqualTo(accommodation.getAddress());
    assertThat(response.detailedAddress()).isEqualTo(accommodation.getDetailedAddress());
    assertThat(response.description()).isEqualTo(accommodation.getDescription());
    assertThat(response.latitude()).isEqualTo(accommodation.getLatitude());
    assertThat(response.longitude()).isEqualTo(accommodation.getLongitude());
    assertThat(response.thumbnailUrl()).isEqualTo(accommodation.getThumbnailUrl());

    // 시설 목록 검증
    assertThat(response.facilityTypes()).hasSize(facilities.size());
    for (int i = 0; i < facilities.size(); i++) {
      assertThat(response.facilityTypes().get(i)).isEqualTo(facilities.get(i).getType().getValue());
    }

    // 반려동물 시설 목록 검증
    assertThat(response.petFacilityTypes()).hasSize(petFacilities.size());
    for (int i = 0; i < petFacilities.size(); i++) {
      assertThat(response.petFacilityTypes().get(i))
          .isEqualTo(petFacilities.get(i).getType().getValue());
    }

    // 허용 반려동물 목록 검증
    assertThat(response.allowPetTypes()).hasSize(allowPets.size());
    for (int i = 0; i < allowPets.size(); i++) {
      assertThat(response.allowPetTypes().get(i)).isEqualTo(
          allowPets.get(i).getPetType().getValue());
    }

    // 추가 이미지 목록 검증
    assertThat(response.additionalImageUrls()).hasSize(accommodationImages.size());
    for (int i = 0; i < accommodationImages.size(); i++) {
      assertThat(response.additionalImageUrls().get(i))
          .isEqualTo(accommodationImages.get(i).getImageUrl());
    }

    verify(accommodationRepository, times(1))
        .findByHostId(host.getId());
    verify(accommodationFacilityRepository, times(1))
        .findAllByAccommodationId(accommodationId);
    verify(accommodationPetFacilityRepository, times(1))
        .findAllByAccommodationId(accommodationId);
    verify(allowPetRepository, times(1))
        .findAllByAccommodationId(accommodationId);
    verify(accommodationImageRepository, times(1))
        .findAllByAccommodationId(accommodationId);
  }

  @Test
  @DisplayName("숙소 조회 - 숙소가 없는 호스트 예외 발생")
  void getAccommodation_WhenAccommodationNotFound_ThrowsException() {
    // given
    Long accommodationNotFoundHostId = 2L;
    when(accommodationRepository.findByHostId(accommodationNotFoundHostId))
        .thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(() -> accommodationService.getAccommodation(accommodationNotFoundHostId))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.ACCOMMODATION_NOT_FOUND);

    verify(accommodationRepository).findByHostId(accommodationNotFoundHostId);
  }

  @Test
  @DisplayName("숙소 수정 - 성공")
  void updateAccommodation_Success() {
    // given
    Long accommodationId = accommodation.getId();
    Long hostId = host.getId();

    when(accommodationRepository.findByHostId(hostId)).thenReturn(Optional.of(accommodation));
    when(accommodationImageRepository.countByAccommodationId(accommodationId)).thenReturn(1);
    when(imageService.storeImage(thumbnail)).thenReturn(THUMBNAIL_URL);

    ArgumentCaptor<List<AccommodationFacility>> facilityCaptor = ArgumentCaptor.forClass(
        List.class);
    when(accommodationFacilityRepository.saveAll(facilityCaptor.capture()))
        .thenReturn(facilities);
    ArgumentCaptor<List<AccommodationPetFacility>> petFacilityCaptor = ArgumentCaptor.forClass(
        List.class);
    when(accommodationPetFacilityRepository.saveAll(petFacilityCaptor.capture()))
        .thenReturn(petFacilities);
    ArgumentCaptor<List<AllowPet>> allowPetCaptor = ArgumentCaptor.forClass(
        List.class);
    when(allowPetRepository.saveAll(allowPetCaptor.capture())).thenReturn(allowPets);

    // when
    accommodationService.updateAccommodation(hostId, updateRequest, thumbnail, additionalImages);

    // then
    verify(accommodationRepository).findByHostId(hostId);
    verify(imageService).storeImage(thumbnail);
    verify(accommodationImageRepository).countByAccommodationId(accommodation.getId());
    verify(accommodationFacilityRepository).deleteAllByAccommodationId(accommodation.getId());
    verify(accommodationPetFacilityRepository).deleteAllByAccommodationId(accommodation.getId());
    verify(allowPetRepository).deleteAllByAccommodationId(accommodation.getId());
  }

  @Test
  @DisplayName("숙소 수정 - 숙소가 존재하지 않는 경우 예외 발생")
  void updateAccommodation_AccommodationNotFound() {
    // given
    when(accommodationRepository.findByHostId(accommodation.getId()))
        .thenReturn(Optional.empty());

    // when
    // then
    assertThatThrownBy(() -> accommodationService
        .updateAccommodation(host.getId(), updateRequest, thumbnail, additionalImages))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.ACCOMMODATION_NOT_FOUND);
  }

  @Test
  @DisplayName("숙소 상세 조회 - 성공")
  void getAccommodationDetail_Success() {
    // given
    Long accommodationId = 1L;
    Long userId = 10L;

    Accommodation accommodation = Accommodation.builder()
        .id(accommodationId)
        .name("가평 블루오션 호텔")
        .address("경기도 가평군 128번지 10010")
        .latitude(37.1234)
        .longitude(127.1234)
        .description("테스트 숙소입니다")
        .type(AccommodationType.PENSION)
        .thumbnailUrl("https://img")
        .totalRating(4.8)
        .build();

    // 명시적 mock 데이터
    List<AccommodationImage> images = List.of(
        AccommodationImage.builder().imageUrl("https://img1").build(),
        AccommodationImage.builder().imageUrl("https://img2").build()
    );
    List<AccommodationFacility> facilities = List.of(
        AccommodationFacility.builder().type(AccommodationFacilityType.WIFI).build()
    );
    List<AccommodationPetFacility> petFacilities = List.of(
        AccommodationPetFacility.builder().type(AccommodationPetFacilityType.SHOWER_ROOM).build()
    );
    List<AllowPet> allowPets = List.of(
        AllowPet.builder().petType(PetType.SMALL_DOG).build()
    );

    Room room = Room.builder()
        .id(100L)
        .name("드럭스 트윈")
        .price(78000L)
        .standardPeopleCount(2)
        .maxPeopleCount(4)
        .standardPetCount(1)
        .maxPetCount(3)
        .extraFee(0L)
        .extraPetFee(0L)
        .extraPeopleFee(0L)
        .checkInTime(LocalTime.of(15, 0))
        .checkOutTime(LocalTime.of(11, 0))
        .imageUrl("https://room.jpg")
        .build();

    Review review = Review.builder()
        .userRating(5.0)
        .petFriendlyRating(4.0)
        .content("너무 좋았어요")
        .createdAt(LocalDateTime.now())
        .build();

    // when
    Mockito.when(accommodationRepository.findById(1L)).thenReturn(Optional.of(accommodation));
    Mockito.when(accommodationImageRepository.findAllByAccommodationId(1L)).thenReturn(images);
    Mockito.when(accommodationFacilityRepository.findAllByAccommodationId(1L))
        .thenReturn(facilities);
    Mockito.when(accommodationPetFacilityRepository.findAllByAccommodationId(1L))
        .thenReturn(petFacilities);
    Mockito.when(allowPetRepository.findAllByAccommodationId(1L)).thenReturn(allowPets);
    Mockito.when(roomRepository.findAllByAccommodationIdOrderByPriceAsc(1L))
        .thenReturn(List.of(room));
    Mockito.when(reviewRepository.findTop5ByAccommodationIdOrderByCreatedAtDesc(1L))
        .thenReturn(List.of(review));
    Mockito.when(wishlistRepository.existsByUserIdAndAccommodationId(userId, accommodationId))
        .thenReturn(true);

    // then
    AccommodationDetailResponse response = accommodationService.getAccommodationDetail(
        accommodationId, userId);

    assertThat(response.getAccommodationId()).isEqualTo(1L);
    assertThat(response.getAccommodationImageUrls()).hasSize(2);
    assertThat(response.getAccommodationFacilities()).contains("와이파이");
    assertThat(response.getAccommodationPetFacilities()).contains("샤워장");
    assertThat(response.getAllowedPets()).contains("소형견");
    assertThat(response.getRoomDetails()).hasSize(1);
    assertThat(response.getReviews()).hasSize(1);
    assertThat(response.getReviews().get(0).getReviewRating()).isEqualTo(4.5); // (5+4)/2
    assertThat(response.isWishlisted()).isTrue();
  }

  @Test
  @DisplayName("숙소 상세 조회 - 존재하지 않는 숙소 ID")
  void getAccommodationDetail_NotFound() {
    // given
    Long accommodationId = 999L;
    Mockito.when(accommodationRepository.findById(accommodationId)).thenReturn(Optional.empty());

    // when
    Throwable thrown = catchThrowable(
        () -> accommodationService.getAccommodationDetail(accommodationId, null));

    // then
    assertThat(thrown).isInstanceOf(MeongnyangerangException.class);
    assertThat(((MeongnyangerangException) thrown).getErrorCode()).isEqualTo(
        ErrorCode.ACCOMMODATION_NOT_FOUND);
  }
}