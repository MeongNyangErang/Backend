package com.meongnyangerang.meongnyangerang.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationImage;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AllowPet;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacility;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacility;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.host.HostStatus;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationCreateRequest;
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

  private Host host;
  private AccommodationCreateRequest request;
  private MockMultipartFile thumbnail;
  private List<MultipartFile> additionalImages;
  private Accommodation accommodation;

  private static final String THUMBNAIL_URL = "https://test.com/image/thumbnail-123.jpg";
  private static final String ADDITIONAL_IMAGE_URL = "https://test.com/image/image1-456.jpg";

  @BeforeEach
  void setUp() {
    // 호스트 객체 생성
    host = Host.builder()
        .id(1L)
        .name("호스트명")
        .email("host@example.com")
        .nickname("호스트닉네임")
        .status(HostStatus.ACTIVE)
        .build();

    List<String> facilities = Arrays.asList("WIFI", "PUBLIC_SWIMMING_POOL");
    List<String> petFacilities = Arrays.asList("PET_FOOD", "EXERCISE_AREA");
    List<String> allowPets = Arrays.asList("SMALL_DOG", "MEDIUM_DOG");

    request = AccommodationCreateRequest.builder()
        .name("test-name")
        .type(AccommodationType.DETACHED_HOUSE)
        .address("test-address")
        .detailedAddress("test-detailedAddress")
        .description("test-description")
        .latitude(37.123)
        .longitude(127.123)
        .facilities(facilities)
        .petFacilities(petFacilities)
        .allowPets(allowPets)
        .build();

    // 이미지 파일 생성
    thumbnail = new MockMultipartFile(
        "thumbnail",
        "thumbnail.jpg",
        "image/jpg",
        "thumbnail content".getBytes()
    );

    // 추가 이미지 생성
    additionalImages = List.of(
        new MockMultipartFile(
            "image1",
            "image1.jpg",
            "image/jpg",
            "image1 content".getBytes())
    );

    // 숙소 객체 생성
    accommodation = Accommodation.builder()
        .id(1L)
        .host(host)
        .name("숙소명")
        .description("숙소 설명")
        .address("서울시 강남구")
        .thumbnailUrl(THUMBNAIL_URL)
        .build();
  }

  @Test
  @DisplayName("숙소 등록 - 성공")
  void createAccommodation_Success() {
    // given
    when(hostRepository.findById(host.getId())).thenReturn(Optional.of(host));
    when(accommodationRepository.existsByHostId(host.getId())).thenReturn(false);

    when(imageService.storeImage(thumbnail)).thenReturn(THUMBNAIL_URL);
    when(imageService.storeImage(additionalImages.get(0))).thenReturn(ADDITIONAL_IMAGE_URL);

    ArgumentCaptor<Accommodation> accommodationCaptor = ArgumentCaptor.forClass(Accommodation.class);
    when(accommodationRepository.save(accommodationCaptor.capture())).thenReturn(accommodation);

    ArgumentCaptor<List<AccommodationFacility>> facilitiesCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<List<AccommodationPetFacility>> petFacilitiesCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<List<AllowPet>> allowPetsCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<List<AccommodationImage>> accommodationImageCaptor = ArgumentCaptor.forClass(List.class);

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
    assertEquals(ADDITIONAL_IMAGE_URL, capturedImages.get(0).getImageUrl());
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

  private Host createNotAuthorizedHost(Long hostId) {
    return Host.builder()
        .id(hostId)
        .status(HostStatus.PENDING)
        .build();
  }
}