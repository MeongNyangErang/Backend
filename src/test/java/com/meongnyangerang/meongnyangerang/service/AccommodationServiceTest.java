package com.meongnyangerang.meongnyangerang.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationImage;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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

  private static final UUID MOCK_UUID = UUID
      .fromString("123e4567-e89b-12d3-a456-426614174000");

  private static final String IMAGE_PATH_PREFIX = "image/";

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

    // 숙소 요청 Mock 생성
    request = mock(AccommodationCreateRequest.class);

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
        .thumbnailUrl(IMAGE_PATH_PREFIX + MOCK_UUID + ".jpg")
        .build();
  }

  @Test
  @DisplayName("숙소 등록 - 성공")
  void createAccommodation_Success() {
    // given
    when(hostRepository.findById(host.getId())).thenReturn(Optional.of(host));
    when(accommodationRepository.existsByHost_Id(host.getId())).thenReturn(false);

    List<String> facilities = Arrays.asList("WIFI", "PUBLIC_SWIMMING_POOL");
    List<String> petFacilities = Arrays.asList("PET_FOOD", "EXERCISE_AREA");
    List<String> allowPets = Arrays.asList("SMALL_DOG", "MEDIUM_DOG");

    when(request.getFacilities()).thenReturn(facilities);
    when(request.getPetFacilities()).thenReturn(petFacilities);
    when(request.getAllowPets()).thenReturn(allowPets);

    // AccommodationCreateRequest.toEntity 모킹
    doReturn(accommodation).when(request).toEntity(any(Host.class), anyString());

    // ArgumentCaptor 설정
    ArgumentCaptor<List<AccommodationFacility>> facilitiesCaptor = ArgumentCaptor
        .forClass(List.class);
    ArgumentCaptor<List<AccommodationPetFacility>> petFacilitiesCaptor = ArgumentCaptor
        .forClass(List.class);
    ArgumentCaptor<List<AllowPet>> allowPetsCaptor = ArgumentCaptor
        .forClass(List.class);
    ArgumentCaptor<List<AccommodationImage>> accommodationImageCaptor = ArgumentCaptor
        .forClass(List.class);

    // UUID 고정 및 파일명 생성 메서드 모킹
    try (MockedStatic<UUID> mockedUUID = mockStatic(UUID.class)) {
      mockedUUID.when(UUID::randomUUID).thenReturn(MOCK_UUID);

      String thumbnailFilename = createFilename(thumbnail.getOriginalFilename());
      Map<String, MultipartFile> additionalImageFilenames =
          createImageFilenameMap(additionalImages);
      List<String> filenames = additionalImageFilenames.keySet().stream().toList();

      // when
      accommodationService.createAccommodation(host.getId(), request, thumbnail, additionalImages);

      // then
      // 호스트 검증 확인
      verify(hostRepository).findById(host.getId());
      verify(accommodationRepository).existsByHost_Id(host.getId());

      // 숙소 저장 확인
      verify(accommodationRepository).save(accommodation);

      // 편의시설 저장 확인
      verify(accommodationFacilityRepository).saveAll(facilitiesCaptor.capture());
      List<AccommodationFacility> capturedFacilities = facilitiesCaptor.getValue();
      assertEquals(2, capturedFacilities.size());

      // 반려동물 편의시설 저장 확인
      verify(accommodationPetFacilityRepository).saveAll(petFacilitiesCaptor.capture());
      List<AccommodationPetFacility> capturedPetFacilities = petFacilitiesCaptor.getValue();
      assertEquals(2, capturedPetFacilities.size());

      // 허용 반려동물 저장 확인
      verify(allowPetRepository).saveAll(allowPetsCaptor.capture());
      List<AllowPet> capturedAllowPets = allowPetsCaptor.getValue();
      assertEquals(2, capturedAllowPets.size());

      // 추가 이미지 저장 확인
      verify(accommodationImageRepository).saveAll(accommodationImageCaptor.capture());
      verify(imageService).storeImage(thumbnail, thumbnailFilename);
      verify(imageService, times(1))
          .storeImage(additionalImages.get(0), filenames.get(0));
    }
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
    when(accommodationRepository.existsByHost_Id(host.getId())).thenReturn(true);

    // when
    // then
    assertThatThrownBy(() -> accommodationService.createAccommodation(
        host.getId(), request, thumbnail, additionalImages))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("ErrorCode", ErrorCode.ACCOMMODATION_ALREADY_EXISTS);

    verify(hostRepository).findById(host.getId());
    verify(accommodationRepository).existsByHost_Id(host.getId());
  }

  private Host createNotAuthorizedHost(Long hostId) {
    return Host.builder()
        .id(hostId)
        .status(HostStatus.PENDING)
        .build();
  }

  private static String createFilename(String originalFilename) {
    String fileName = UUID.randomUUID() + getExtension(originalFilename);
    return IMAGE_PATH_PREFIX + fileName;
  }

  private Map<String, MultipartFile> createImageFilenameMap(List<MultipartFile> images) {
    return images.stream()
        .collect(Collectors.toMap(
            image -> createFilename(image.getOriginalFilename()),
            image -> image
        ));
  }

  private static String getExtension(String originalFileName) {
    try {
      return originalFileName.substring(originalFileName.lastIndexOf("."));
    } catch (StringIndexOutOfBoundsException e) {
      throw new MeongnyangerangException(ErrorCode.INVALID_EXTENSION);
    }
  }
}