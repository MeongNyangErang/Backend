package com.meongnyangerang.meongnyangerang.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationImageRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationPetFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AllowPetRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class AccommodationServiceTest {

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

 /* @Mock
  private ImageService imagerService; // TODO

  @Mock
  private HostService hostService;*/ // TODO

  @InjectMocks
  private AccommodationService accommodationService;

  private Host host;
  private AccommodationCreateRequest request;
  private MockMultipartFile thumbnail;
  private List<MultipartFile> additionalImages;
  private Accommodation accommodation;

  @BeforeEach
  void setUp() {
    // 호스트 생성
    host = Host.builder()
        .id(1L)
        .email("test@gmail.com")
        .name("test-name")
        .nickname("test-nickname")
        .password("test-password")
        .profileImageUrl("/test/profile/image.jpg")
        .businessLicenseImageUrl("/test/business/license/image.jpg")
        .submitDocumentImageUrl("/test/submit/document/image/jpg")
        .phoneNumber("01012345678")
        .status(HostStatus.ACTIVE)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    // 테스트용 요청 DTO 생성
    request = AccommodationCreateRequest.builder()
        .name("테스트 숙소")
        .type(AccommodationType.PENSION)
        .address("제주시 노형구")
        .detailedAddress("노형동")
        .description("제주 감귤")
        .latitude(37.123)
        .longitude(127.123)
        .facilities(Arrays.asList("WIFI", "SWIMMING_POOL"))
        .petFacilities(Arrays.asList("PET_FOOD", "EXERCISE_AREA"))
        .allowedPets(Arrays.asList("SMALL_DOG", "CAT"))
        .build();

    // 테스트용 썸네일 이미지 파일 생성
    thumbnail = new MockMultipartFile(
        "thumbnail",
        "thumbnail.jpg",
        "image/jpeg",
        "thumbnail image".getBytes()
    );

    // 테스트용 추가 이미지 파일 생성
    MockMultipartFile image1 = new MockMultipartFile(
        "image1",
        "test1.jpg",
        "image/jpg",
        "additional image 1".getBytes()
    );

    MockMultipartFile image2 = new MockMultipartFile(
        "image2",
        "test2.jpg",
        "image/jpg",
        "additional image 2".getBytes()
    );

    // 다중 이미지 파일
    additionalImages = Arrays.asList(thumbnail, image1, image2);

    // 숙소 엔티티 생성
    accommodation = Accommodation.builder()
        .id(1L)
        .host(host)
        .name(request.getName())
        .description(request.getDescription())
        .address(request.getAddress())
        .detailedAddress(request.getDetailedAddress())
        .latitude(request.getLatitude())
        .longitude(request.getLongitude())
        .type(request.getType())
        .thumbnailUrl("/test/accommodations/thumbnail.jpg")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  @Test
  @DisplayName("숙소 등록 - 성공")
  void createAccommodation_Success() {
    // given
    //when(hostRepository.findById(host.getId())).thenReturn(host); // TODO
    when(accommodationRepository.existsByHost_Id(host.getId())).thenReturn(false);

    // 썸네일 + 추가 이미지 한번에 업로드
    /*when(imagerService.storeImageFiles(additionalImages)).thenReturn(List.of(
        "/test/thumbnail.jpg",
        "/test/additional-image1.jpg",
        "/test/additional-image2.jpg"
    ));*/

    when(accommodationRepository.save(accommodation)).thenReturn(accommodation);

    // AccommodationImage 저장 Mocking
    AccommodationImage accommodationImage1 = AccommodationImage.builder()
        .id(1L)
        .accommodation(accommodation)
        .imageUrl("/test/additional-image1.jpg")
        .createdAt(LocalDateTime.now())
        .build();

    AccommodationImage accommodationImage2 = AccommodationImage.builder()
        .id(2L)
        .accommodation(accommodation)
        .imageUrl("/test/additional-image2.jpg")
        .createdAt(LocalDateTime.now())
        .build();

    when(accommodationImageRepository.save(accommodationImage1)).thenReturn(accommodationImage1);
    when(accommodationImageRepository.save(accommodationImage2)).thenReturn(accommodationImage2);

    // 편의시설, 반려동물 편의시설, 허용 반려동물 Mocking
    AccommodationFacility accommodationFacility = AccommodationFacility.builder()
        .id(1L)
        .accommodation(accommodation)
        .type(AccommodationFacilityType.WIFI)
        .build();

    AccommodationPetFacility accommodationPetFacility1 = AccommodationPetFacility.builder()
        .id(1L)
        .accommodation(accommodation)
        .type(AccommodationPetFacilityType.PET_FOOD)
        .build();

    AccommodationPetFacility accommodationPetFacility2 = AccommodationPetFacility.builder()
        .id(2L)
        .accommodation(accommodation)
        .type(AccommodationPetFacilityType.EXERCISE_AREA)
        .build();

    AllowPet allowPet1 = AllowPet.builder()
        .id(1L)
        .accommodation(accommodation)
        .petType(PetType.SMALL_DOG)
        .build();

    AllowPet allowPet2 = AllowPet.builder()
        .id(2L)
        .accommodation(accommodation)
        .petType(PetType.CAT)
        .build();

    when(accommodationFacilityRepository.save(accommodationFacility))
        .thenReturn(accommodationFacility); // 숙소 편의시설

    when(accommodationPetFacilityRepository.save(accommodationPetFacility1))
        .thenReturn(accommodationPetFacility1); // 숙소 반려동물 편의시설 1
    when(accommodationPetFacilityRepository.save(accommodationPetFacility2))
        .thenReturn(accommodationPetFacility2); // 숙소 반려동물 편의시설 2

    when(allowPetRepository.save(allowPet1)).thenReturn(allowPet1); // 숙소 허용 반려동물 1
    when(allowPetRepository.save(allowPet2)).thenReturn(allowPet2); // 숙소 허용 반려동물 2

    // when
    accommodationService.createAccommodation(host.getId(), request, thumbnail, additionalImages);

    // then
    //verify(hostRepository, times(1)).findById(host.getId()); // TODO
    verify(accommodationRepository, times(1))
        .existsByHost_Id(host.getId());

    //verify(imagerService, times(1)).storageImageFiles(additionalImages); // TODO

    verify(accommodationRepository, times(1)).save(accommodation);

    verify(accommodationImageRepository, times(1)).save(accommodationImage1);
    verify(accommodationImageRepository, times(1)).save(accommodationImage2);

    verify(accommodationFacilityRepository, times(1)).save(accommodationFacility);
    verify(accommodationFacilityRepository, times(1)).save(accommodationFacility);

    verify(accommodationPetFacilityRepository, times(1))
        .save(accommodationPetFacility1);
    verify(accommodationPetFacilityRepository, times(2))
        .save(accommodationPetFacility2);

    verify(allowPetRepository, times(1)).save(allowPet1);
    verify(allowPetRepository, times(1)).save(allowPet2);
  }

  @Test
  @DisplayName("숙소 등록 실패  - 이미 등록된 경우 409 에러")
  void createAccommodation_WhenAlreadyRegistered_ThrowsException() {
    // given
    //when(hostRepository.findById(host.getId())).thenReturn(host); // TODO
    when(accommodationRepository.existsByHost_Id(host.getId())).thenReturn(true);

    // when
    // then
    /*assertThatThrownBy(() ->
        accommodationService.createAccommodation(host.getId(), request, thumbnail, additionalImages))
        .isInstanceOf(RuntimeException.class) // TODO: CustomException 으로 변경 409
        .hasFieldOrPropertyWithValue(""); // TODO*/
  }

  @Test
  @DisplayName("숙소 등록 실패 - 승인된 호스트가 아닐 경우 403 에러")
  void createAccommodation_WhenNotAuthorizedHost_ThrowsException() {
    // given
    //when(hostRepository.findById(host.getId())).thenReturn(host); // TODO
    Host notAuthorizedHost = createNotAuthorizedHost(1L);

    // when
    // then
    /*assertThatThrownBy(() ->
        accommodationService.createAccommodation(
            notAuthorizedHost.getId(), request, thumbnail, additionalImages)
    ).isInstanceOf(RuntimeException.class) // TODO: CustomException 으로 변경 403
        .hasFieldOrPropertyWithValue(""); // TODO*/
  }

  private Host createNotAuthorizedHost(Long hostId) {
    return Host.builder()
        .id(hostId)
        .status(HostStatus.PENDING)
        .build();
  }
}