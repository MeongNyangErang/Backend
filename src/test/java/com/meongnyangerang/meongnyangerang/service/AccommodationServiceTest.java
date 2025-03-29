package com.meongnyangerang.meongnyangerang.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationCreateRequest;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationImageRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationPetFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AllowPetRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
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

  @InjectMocks
  private AccommodationService accommodationService;

  private static final String THUMBNAIL_URL = "https://test.com/image/thumbnail-123.jpg";
  private static final String ADDITIONAL_IMAGE_URL1 = "https://test.com/image/image1-456.jpg";
  private static final String ADDITIONAL_IMAGE_URL2 = "https://test.com/image/image2-456.jpg";

  private static final List<AccommodationFacilityType> FACILITY_TYPES = Arrays
      .asList(AccommodationFacilityType.WIFI, AccommodationFacilityType.PUBLIC_SWIMMING_POOL);
  private static final List<AccommodationPetFacilityType> PET_FACILITY_TYPES = Arrays
      .asList(AccommodationPetFacilityType.PET_FOOD, AccommodationPetFacilityType.EXERCISE_AREA);
  private static final List<PetType> PET_TYPES = Arrays
      .asList(PetType.SMALL_DOG, PetType.MEDIUM_DOG);

  private Host host;
  private AccommodationCreateRequest request;
  private MockMultipartFile thumbnail;
  private List<MultipartFile> additionalImages;

  private Accommodation accommodation;
  private List<AccommodationFacility> facilities;
  private List<AccommodationPetFacility> petFacilities;
  private List<AllowPet> allowPets;
  private List<AccommodationImage> accommodationImages;


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
        .id(1L)
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

    request = AccommodationCreateRequest.builder()
        .name("test-name")
        .type(AccommodationType.DETACHED_HOUSE)
        .address("test-address")
        .detailedAddress("test-detailedAddress")
        .description("test-description")
        .latitude(37.123)
        .longitude(127.123)
        .facilities(FACILITY_TYPES)
        .petFacilities(PET_FACILITY_TYPES)
        .allowPets(PET_TYPES)
        .build();

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
        "thumbnail content".getBytes()
    );

    additionalImages = List.of(
        new MockMultipartFile(
            "image1",
            "image1.jpg",
            "image/jpg",
            "image1 content".getBytes())
    );
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
  @DisplayName("숙소 등록 - 권한 없음")
  void createAccommodation_NotAuthorized_ThrowsException() {
    // given
    host = createNotAuthorizedHost(2L);
    when(hostRepository.findById(host.getId())).thenReturn(Optional.of(host));

    // when
    // then
    assertThatThrownBy(() -> accommodationService.createAccommodation(
        host.getId(), request, thumbnail, additionalImages))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.INVALID_AUTHORIZED);

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
    assertThat(response.facilities()).hasSize(facilities.size());
    for (int i = 0; i < facilities.size(); i++) {
      assertThat(response.facilities().get(i)).isEqualTo(facilities.get(i).getType().getValue());
    }

    // 반려동물 시설 목록 검증
    assertThat(response.petFacilities()).hasSize(petFacilities.size());
    for (int i = 0; i < petFacilities.size(); i++) {
      assertThat(response.petFacilities().get(i))
          .isEqualTo(petFacilities.get(i).getType().getValue());
    }

    // 허용 반려동물 목록 검증
    assertThat(response.allowPets()).hasSize(allowPets.size());
    for (int i = 0; i < allowPets.size(); i++) {
      assertThat(response.allowPets().get(i)).isEqualTo(allowPets.get(i).getPetType().getValue());
    }

    // 추가 이미지 목록 검증
    assertThat(response.additionalImages()).hasSize(accommodationImages.size());
    for (int i = 0; i < accommodationImages.size(); i++) {
      assertThat(response.additionalImages().get(i))
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


  private Host createNotAuthorizedHost(Long hostId) {
    return Host.builder()
        .id(hostId)
        .status(HostStatus.PENDING)
        .build();
  }
}