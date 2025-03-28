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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private List<String> successUploadImages;

  /**
   * 숙소 등록
   */
  @Transactional
  public void createAccommodation(
      Long hostId,
      AccommodationCreateRequest request,
      MultipartFile thumbnail,
      List<MultipartFile> additionalImages
  ) {
    additionalImages = additionalImages == null ? Collections.emptyList() : additionalImages;
    Host host = validateHost(hostId);

    String filename = createFilename(thumbnail.getOriginalFilename());
    Map<String, MultipartFile> imageFilenameMap = createImageFilenameMap(additionalImages);

    Accommodation accommodation = request.toEntity(host, filename);
    accommodationRepository.save(accommodation);

    saveAccommodationFacilities(request.getFacilities(), accommodation);
    saveAccommodationPetFacilities(request.getPetFacilities(), accommodation);
    saveAllowPets(request.getAllowPets(), accommodation);
    saveAdditionalImages(imageFilenameMap.keySet(), accommodation);

    storeImageProcess(thumbnail, filename, imageFilenameMap); // S3 이미지 저장
  }

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

  private void saveAdditionalImages(Set<String> filenames, Accommodation accommodation) {
    List<AccommodationImage> accommodationImages = filenames.stream()
        .map(filename -> AccommodationImage.builder()
            .accommodation(accommodation)
            .imageUrl(filename)
            .build())
        .toList();

    accommodationImageRepository.saveAll(accommodationImages);
  }

  private void saveAccommodationFacilities(
      List<String> facilityTypes, Accommodation accommodation
  ) {
    List<AccommodationFacility> facilities = facilityTypes.stream()
        .map(facilityType -> {
          AccommodationFacilityType type =
              AccommodationFacilityType.valueOf(facilityType.toUpperCase());

          return AccommodationFacility.builder()
              .accommodation(accommodation)
              .type(type)
              .build();
        })
        .collect(Collectors.toList());

    accommodationFacilityRepository.saveAll(facilities);
  }

  private void saveAccommodationPetFacilities(
      List<String> petFacilityTypes, Accommodation accommodation
  ) {
    List<AccommodationPetFacility> petFacilities = petFacilityTypes.stream()
        .map(petFacilityType -> {
          AccommodationPetFacilityType type =
              AccommodationPetFacilityType.valueOf(petFacilityType.toUpperCase());

          return AccommodationPetFacility.builder()
              .accommodation(accommodation)
              .type(type)
              .build();
        })
        .collect(Collectors.toList());

    accommodationPetFacilityRepository.saveAll(petFacilities);
  }

  private void saveAllowPets(List<String> petTypes, Accommodation accommodation) {
    List<AllowPet> allowPets = petTypes.stream()
        .map(petType -> {
          PetType type = PetType.valueOf(petType.toUpperCase());

          return AllowPet.builder()
              .accommodation(accommodation)
              .petType(type)
              .build();
        })
        .collect(Collectors.toList());

    allowPetRepository.saveAll(allowPets);
  }

  private void storeImageProcess(
      MultipartFile thumbnail,
      String filename,
      Map<String, MultipartFile> imageFilenameMap
  ) {
    try {
      successUploadImages = new ArrayList<>();
      String successUploadThumbnail = imageService.storeImage(thumbnail, filename);
      successUploadImages.add(successUploadThumbnail);
      storeAdditionalImages(imageFilenameMap);
    } catch (Exception e) {
      log.error("숙소 등록 에러 발생, S3에 업로드된 이미지 삭제");
      imageService.deleteImages(successUploadImages);
      throw new MeongnyangerangException(ErrorCode.REGISTRATION_ACCOMMODATION);
    }
  }

  private void storeAdditionalImages(Map<String, MultipartFile> imageFilenameMap) {
    imageFilenameMap.forEach((filename, image) -> {
          String successUploadImage = imageService.storeImage(image, filename);
          successUploadImages.add(successUploadImage);
        }
    );
  }

  private Map<String, MultipartFile> createImageFilenameMap(List<MultipartFile> images) {
    return images.stream()
        .collect(Collectors.toMap(
            image -> createFilename(image.getOriginalFilename()),
            image -> image
        ));
  }

  private String createFilename(String originalFilename) {
    String fileName = UUID.randomUUID() + getExtension(originalFilename);
    return IMAGE_PATH_PREFIX + fileName;
  }

  private String getExtension(String originalFileName) {
    try {
      return originalFileName.substring(originalFileName.lastIndexOf("."));
    } catch (StringIndexOutOfBoundsException e) {
      log.error("파일의 확장자가 없습니다.");
      throw new MeongnyangerangException(ErrorCode.INVALID_EXTENSION);
    }
  }
}
