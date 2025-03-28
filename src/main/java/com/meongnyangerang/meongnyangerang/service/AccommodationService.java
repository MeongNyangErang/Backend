package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationImage;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AllowPet;
import com.meongnyangerang.meongnyangerang.domain.accommodation.PetType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacility;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacilityType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacility;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacilityType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccommodationService {

  private static final String IMAGE_PATH_PREFIX = "image/";

  private final HostRepository hostRepository;
  private final AccommodationRepository accommodationRepository;
  private final AccommodationImageRepository accommodationImageRepository;
  private final AccommodationFacilityRepository accommodationFacilityRepository;
  private final AccommodationPetFacilityRepository accommodationPetFacilityRepository;
  private final AllowPetRepository allowPetRepository;
  private final ImageService imageService;

  /**
   * 숙소 등록
   *
   * @param hostId           호스트 ID
   * @param request          숙소 등록 정보
   * @param thumbnail        숙소 대표 이미지
   * @param additionalImages 추가 이미지
   */
  @Transactional
  public void createAccommodation(
      Long hostId,
      AccommodationCreateRequest request,
      MultipartFile thumbnail,
      List<MultipartFile> additionalImages
  ) {
    // 호스트 검증
    Host host = validateHost(hostId);

    // 파일 이름 생성
    String filename = createFilename(thumbnail.getOriginalFilename());
    List<String> filenames = createFilenames(additionalImages);

    // 숙소 엔티티 생성
    Accommodation accommodation = request.toEntity(host, filename);

    // 숙소 저장
    accommodationRepository.save(accommodation);

    // 편의시설 정보 저장
    saveAccommodationFacilities(request.getFacilities(), accommodation);

    // 반려동물 편의시설 정보 저장
    saveAccommodationPetFacilities(request.getPetFacilities(), accommodation);

    // 허용 반려동물 정보 저장
    saveAllowPets(request.getAllowPets(), accommodation);

    // 추가 이미지 저장
    saveAdditionalImages(filenames, accommodation);

    // S3 이미지 파일 업로드
    // DB 트랜잭션을 위해 맨 마지막에 배치
    storeImageProcess(thumbnail, filename, additionalImages, filenames);
  }

  /**
   * 호스트 검증
   *
   * @param hostId 호스트 ID
   * @return 검증된 호스트 객체
   */
  private Host validateHost(Long hostId) {
    // 호스트 존재하는지 검증
    Host host = hostRepository.findById(hostId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.NOT_EXISTS_HOST));

    // 유효한 호스트 상태인지 검증
    HostStatus.isStatusByCreateAccommodation(host.getStatus());

    // 개설한 숙소가 있는지 검증
    if (accommodationRepository.existsByHost_Id(hostId)) {
      throw new MeongnyangerangException(ErrorCode.ACCOMMODATION_ALREADY_EXISTS);
    }
    return host;
  }

  /**
   * 추가 이미지 URL DB 저장
   *
   * @param filenames     추가 이미지 URL
   * @param accommodation 숙소 객체
   */
  private void saveAdditionalImages(@Nullable List<String> filenames, Accommodation accommodation) {
    Optional.ofNullable(filenames)
        .filter(names -> !names.isEmpty())
        .ifPresent(names -> {
          List<AccommodationImage> accommodationImages = new ArrayList<>();

          for (String filename : filenames) {
            AccommodationImage accommodationImage = AccommodationImage.builder()
                .accommodation(accommodation)
                .imageUrl(filename)
                .build();

            accommodationImages.add(accommodationImage);
          }
          accommodationImageRepository.saveAll(accommodationImages);
        });
  }

  /**
   * 숙소 편의시설 저장
   */
  private void saveAccommodationFacilities(
      @Nullable List<String> facilityTypes, Accommodation accommodation
  ) {
    Optional.ofNullable(facilityTypes)
        .filter(types -> !types.isEmpty())
        .ifPresent(types -> {
          List<AccommodationFacility> facilities = new ArrayList<>();

          for (String facilityType : types) {
            AccommodationFacilityType type =
                AccommodationFacilityType.valueOf(facilityType.toUpperCase());

            AccommodationFacility facility = AccommodationFacility.builder()
                .accommodation(accommodation)
                .type(type)
                .build();

            facilities.add(facility);
          }
          accommodationFacilityRepository.saveAll(facilities);
        });
  }

  /**
   * 숙소 반려동물 편의시설 저장
   */
  private void saveAccommodationPetFacilities(
      @Nullable List<String> petFacilityTypes, Accommodation accommodation
  ) {
    Optional.ofNullable(petFacilityTypes)
        .filter(types -> !types.isEmpty())
        .ifPresent(types -> {
          List<AccommodationPetFacility> petFacilities = new ArrayList<>();

          for (String petFacilityType : petFacilityTypes) {
            AccommodationPetFacilityType type =
                AccommodationPetFacilityType.valueOf(petFacilityType.toUpperCase());

            AccommodationPetFacility petFacility = AccommodationPetFacility.builder()
                .accommodation(accommodation)
                .type(type)
                .build();

            petFacilities.add(petFacility);
          }
          accommodationPetFacilityRepository.saveAll(petFacilities);
        });
  }

  /**
   * 허용 반려동물 정보 저장
   */
  private void saveAllowPets(List<String> petTypes, Accommodation accommodation) {
    if (petTypes == null || petTypes.isEmpty()) {
      throw new MeongnyangerangException(ErrorCode.EMPTY_PET_TYPE);
    }

    List<AllowPet> allowPets = new ArrayList<>();

    for (String petType : petTypes) {
      PetType type = PetType.valueOf(petType.toUpperCase());

      AllowPet allowPet = AllowPet.builder()
          .accommodation(accommodation)
          .petType(type)
          .build();

      allowPets.add(allowPet);
    }

    allowPetRepository.saveAll(allowPets);
  }

  private void storeImageProcess(
      MultipartFile thumbnail,
      String filename,
      @Nullable List<MultipartFile> additionalImages,
      @Nullable List<String> filenames
  ) {
    imageService.storeImage(thumbnail, filename);
    storeAdditionalImagesProcess(additionalImages, filenames);
  }

  private void storeAdditionalImagesProcess(
      @Nullable List<MultipartFile> additionalImages,
      @Nullable List<String> filenames
  ) {
    Optional.ofNullable(additionalImages)
        .filter(images -> !images.isEmpty())
        .ifPresent(images -> {
          List<String> additionalImageUrls = Optional.ofNullable(filenames)
              .filter(names -> !names.isEmpty())
              .orElseThrow(() -> new MeongnyangerangException(ErrorCode.INTERNAL_SERVER_ERROR));

          validateImageCount(images, additionalImageUrls);
          storeAdditionalImages(images, additionalImageUrls);
        });
  }

  private static void validateImageCount(
      List<MultipartFile> images, List<String> additionalImageUrls
  ) {
    if (images.size() != additionalImageUrls.size()) {
      throw new MeongnyangerangException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  private void storeAdditionalImages(
      List<MultipartFile> images, List<String> additionalImageUrls
  ) {
    for (int i = 0; i < images.size(); i++) {
      imageService.storeImage(images.get(i), additionalImageUrls.get(i));
    }
  }

  private static String createFilename(String originalFilename) {
    String fileName = UUID.randomUUID() + getExtension(originalFilename);
    return IMAGE_PATH_PREFIX + fileName;
  }

  private static List<String> createFilenames(@Nullable List<MultipartFile> additionalImages) {
    if (additionalImages != null && !additionalImages.isEmpty()) {
      return additionalImages.stream()
          .map(image -> createFilename(image.getOriginalFilename()))
          .toList();
    }
    return null;
  }

  private static String getExtension(String originalFileName) {
    try {
      return originalFileName.substring(originalFileName.lastIndexOf("."));
    } catch (StringIndexOutOfBoundsException e) {
      log.error("파일의 확장자가 없습니다.");
      throw new MeongnyangerangException(ErrorCode.INVALID_EXTENSION);
    }
  }
}
