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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  private final HostRepository hostRepository;
  private final AccommodationRepository accommodationRepository;
  private final AccommodationFacilityRepository accommodationFacilityRepository;
  private final AccommodationPetFacilityRepository accommodationPetFacilityRepository;
  private final AllowPetRepository allowPetRepository;
  private final AccommodationImageRepository accommodationImageRepository;
  private final ImageService imageService;

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

    List<String> uploadedImageUrls = new ArrayList<>(); // 업로드 성공한 이미지 추적

    try {
      String thumbnailUrl = imageService.storeImage(thumbnail);
      uploadedImageUrls.add(thumbnailUrl);

      Map<String, MultipartFile> imageUrlMap =
          createImageUrlMap(additionalImages, uploadedImageUrls);

      Accommodation accommodation = request.toEntity(host, thumbnailUrl);
      accommodationRepository.save(accommodation);

      saveAccommodationFacilities(request.getFacilities(), accommodation);
      saveAccommodationPetFacilities(request.getPetFacilities(), accommodation);
      saveAllowPets(request.getAllowPets(), accommodation);
      saveAdditionalImages(imageUrlMap.keySet(), accommodation);

      log.info("숙소 등록 완료: 호스트 ID={}, 숙소 ID={}", hostId, accommodation.getId());
    } catch (Exception e) {
      log.error("숙소 등록 에러 발생, S3에 업로드된 이미지 삭제");
      imageService.deleteImages(uploadedImageUrls);
      throw new MeongnyangerangException(ErrorCode.REGISTRATION_ACCOMMODATION);
    }
  }

  /**
   * 숙소 조회
   */
  public AccommodationResponse getAccommodation(Long hostId) {
    Accommodation accommodation = accommodationRepository.findByHostId(hostId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.ACCOMMODATION_NOT_FOUND));

   Long accommodationId = accommodation.getId();

    List<AccommodationFacility> facilities = accommodationFacilityRepository
        .findAllByAccommodationId(accommodationId);

    List<AccommodationPetFacility> petFacilities = accommodationPetFacilityRepository
        .findAllByAccommodationId(accommodationId);

    List<AllowPet> allowPets = allowPetRepository.findAllByAccommodationId(accommodationId);

    List<AccommodationImage> additionalImages = accommodationImageRepository
        .findAllByAccommodationId(accommodationId);

    return AccommodationResponse.of(
        accommodation,
        facilities,
        petFacilities,
        allowPets,
        additionalImages
    );
  }

  private Map<String, MultipartFile> createImageUrlMap(
      List<MultipartFile> additionalImages,
      List<String> uploadedImageUrls
  ) {
    Map<String, MultipartFile> imageUrlMap = new HashMap<>();

    for (MultipartFile image : additionalImages) {
      String imageUrl = imageService.storeImage(image);
      imageUrlMap.put(imageUrl, image);
      uploadedImageUrls.add(imageUrl);
    }
    return imageUrlMap;
  }

  private Host validateHost(Long hostId) {
    // 호스트 존재하는지 검증
    Host host = hostRepository.findById(hostId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.NOT_EXISTS_HOST));

    // 유효한 호스트 상태인지 검증
    HostStatus.isStatusByCreateAccommodation(host.getStatus());

    // 개설한 숙소가 있는지 검증
    if (accommodationRepository.existsByHostId(hostId)) {
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
      List<AccommodationFacilityType> facilityTypes, Accommodation accommodation
  ) {
    List<AccommodationFacility> facilities = facilityTypes.stream()
        .map(facilityType -> AccommodationFacility.builder()
            .accommodation(accommodation)
            .type(facilityType)
            .build())
        .collect(Collectors.toList());

    accommodationFacilityRepository.saveAll(facilities);
  }

  private void saveAccommodationPetFacilities(
      List<AccommodationPetFacilityType> petFacilityTypes, Accommodation accommodation
  ) {
    List<AccommodationPetFacility> petFacilities = petFacilityTypes.stream()
        .map(petFacilityType -> AccommodationPetFacility.builder()
            .accommodation(accommodation)
            .type(petFacilityType)
            .build())
        .collect(Collectors.toList());

    accommodationPetFacilityRepository.saveAll(petFacilities);
  }

  private void saveAllowPets(List<PetType> petTypes, Accommodation accommodation) {
    List<AllowPet> allowPets = petTypes.stream()
        .map(petType -> AllowPet.builder()
            .accommodation(accommodation)
            .petType(petType)
            .build())
        .collect(Collectors.toList());

    allowPetRepository.saveAll(allowPets);
  }
}
