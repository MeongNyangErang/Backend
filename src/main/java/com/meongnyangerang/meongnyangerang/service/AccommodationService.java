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
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationCreateRequest;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationResponse;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationUpdateRequest;
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
import java.util.Optional;
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
  public void createAccommodation(
      Long hostId,
      AccommodationCreateRequest request,
      MultipartFile thumbnail,
      List<MultipartFile> additionalImages
  ) {
    Host host = validateHost(hostId);
    additionalImages = initAdditionalImages(additionalImages);

    List<String> trackingList = new ArrayList<>(); // 업로드 성공한 이미지 추적

    try {
      String thumbnailUrl = uploadImage(thumbnail, trackingList);
      List<String> newAdditionalImageUrls = uploadImages(additionalImages, trackingList);

      Accommodation accommodation = request.toEntity(host, thumbnailUrl);
      saveEntities(request, accommodation, newAdditionalImageUrls);

      log.info("숙소 등록 성공, 호스트 ID : {}, 숙소 ID : {}", hostId, accommodation.getId());
    } catch (Exception e) {
      log.error("숙소 등록 에러 발생, S3에 업로드된 이미지 삭제, 원인: {}", e.getMessage());
      imageService.deleteImages(trackingList);
      throw new MeongnyangerangException(ErrorCode.REGISTRATION_ACCOMMODATION);
    }
  }

  @Transactional
  public void saveEntities(
      AccommodationCreateRequest request,
      Accommodation accommodation,
      List<String> newAdditionalImageUrls
  ) {
    accommodationRepository.save(accommodation);
    saveAccommodationFacilities(request.facilityTypes(), accommodation);
    saveAccommodationPetFacilities(request.petFacilityTypes(), accommodation);
    saveAllowPets(request.allowPetTypes(), accommodation);
    saveAdditionalImages(newAdditionalImageUrls, accommodation);
  }

  /**
   * 숙소 조회
   */
  public AccommodationResponse getAccommodation(Long hostId) {
    Accommodation accommodation = findAccommodationByHostId(hostId);
    return createAccommodationResponse(accommodation);
  }

  /**
   * 숙소 수정
   */
  @Transactional
  public AccommodationResponse updateAccommodation(
      Long hostId,
      AccommodationUpdateRequest request,
      MultipartFile thumbnail,
      List<MultipartFile> newAdditionalImages
  ) {
    Accommodation accommodation = findAccommodationByHostId(hostId);
    newAdditionalImages = initAdditionalImages(newAdditionalImages);

    List<String> trackingList = new ArrayList<>(); // 업로드 성공한 이미지 추적 (롤백 위함)
    List<String> oldImageUrlsToDelete = new ArrayList<>(); // 삭제할 이미지 추적

    try {
      String newThumbnailUrl = processThumbnailUpdate(
          accommodation.getThumbnailUrl(), thumbnail, trackingList, oldImageUrlsToDelete);

      List<String> newAdditionalImageUrls = processAdditionalImagesUpdate(
          accommodation, newAdditionalImages, trackingList, oldImageUrlsToDelete);

      updateEntities(accommodation, request, newThumbnailUrl, newAdditionalImageUrls);
      imageService.registerImagesForDeletion(oldImageUrlsToDelete);

      log.info("숙소 수정 성공, 숙소 ID : {}", accommodation.getId());
      return createAccommodationResponse(accommodation);
    } catch (Exception e) {
      log.error("숙소 수정 에러 발생, S3에 업로드된 이미지 삭제 처리, 원인: {}", e.getMessage());
      imageService.deleteImages(trackingList);
      throw new MeongnyangerangException(ErrorCode.REGISTRATION_ACCOMMODATION);
    }
  }

  private String uploadImage(MultipartFile thumbnail, List<String> trackingList) {
    String thumbnailUrl = imageService.storeImage(thumbnail);
    trackingList.add(thumbnailUrl);
    return thumbnailUrl;
  }

  private String processThumbnailUpdate(
      String oldThumbnailUrl,
      MultipartFile newThumbnail,
      List<String> trackingList,
      List<String> oldImageUrlsToDelete
  ) {
    String newThumbnailUrl = oldThumbnailUrl;

    if (newThumbnail != null && !newThumbnail.isEmpty()) {
      newThumbnailUrl = uploadImage(newThumbnail, trackingList);
      oldImageUrlsToDelete.add(oldThumbnailUrl);
    }

    return newThumbnailUrl;
  }

  private List<String> processAdditionalImagesUpdate(
      Accommodation accommodation,
      List<MultipartFile> newAdditionalImages,
      List<String> trackingList,
      List<String> oldImageUrlsToDelete
  ) {
    if (newAdditionalImages.isEmpty()) {
      return List.of();
    }
    List<String> newAdditionalImageUrls = uploadImages(newAdditionalImages, trackingList);

    List<AccommodationImage> existingImages = accommodationImageRepository
        .findAllByAccommodationId(accommodation.getId());

    for (AccommodationImage image : existingImages) {
      oldImageUrlsToDelete.add(image.getImageUrl());
    }

    return newAdditionalImageUrls;
  }

  private AccommodationResponse createAccommodationResponse(Accommodation accommodation) {
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

  private Accommodation findAccommodationByHostId(Long hostId) {
    return accommodationRepository.findByHostId(hostId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.ACCOMMODATION_NOT_FOUND));
  }

  private void updateEntities(
      Accommodation accommodation,
      AccommodationUpdateRequest request,
      String newThumbnailUrl,
      List<String> newAdditionalImageUrls
  ) {
    Long accommodationId = accommodation.getId();

    accommodation.updateAccommodation(request, newThumbnailUrl);
    updateFacilities(accommodationId, request.facilityTypes(), accommodation);

    updatePetFacilities(accommodationId, request.petFacilityTypes(), accommodation);
    updateAllowPets(accommodationId, request.allowPetTypes(), accommodation);

    if (!newAdditionalImageUrls.isEmpty()) {
      accommodationImageRepository.deleteAllByAccommodationId(accommodationId);
      saveAdditionalImages(newAdditionalImageUrls, accommodation);
    }
  }

  private void updateFacilities(
      Long accommodationId,
      List<AccommodationFacilityType> newFacilityTypes,
      Accommodation accommodation
  ) {
    accommodationFacilityRepository.deleteAllByAccommodationId(accommodationId);
    saveAccommodationFacilities(newFacilityTypes, accommodation);
  }

  private void updatePetFacilities(
      Long accommodationId,
      List<AccommodationPetFacilityType> newPetFacilityTypes,
      Accommodation accommodation
  ) {
    accommodationPetFacilityRepository.deleteAllByAccommodationId(accommodationId);
    saveAccommodationPetFacilities(newPetFacilityTypes, accommodation);
  }

  private void updateAllowPets(
      Long accommodationId,
      List<PetType> newPetTypes,
      Accommodation accommodation
  ) {
    allowPetRepository.deleteAllByAccommodationId(accommodationId);
    saveAllowPets(newPetTypes, accommodation);
  }

  private List<String> uploadImages(
      List<MultipartFile> additionalImages,
      List<String> uploadedImageUrls
  ) {
    List<String> imageUrls = new ArrayList<>();

    for (MultipartFile image : additionalImages) {
      String imageUrl = uploadImage(image, imageUrls);
      uploadedImageUrls.add(imageUrl);
    }

    return imageUrls;
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

  private void saveAdditionalImages(List<String> filenames, Accommodation accommodation) {
    List<AccommodationImage> accommodationImages = filenames.stream()
        .map(filename -> AccommodationImage.builder()
            .accommodation(accommodation)
            .imageUrl(filename)
            .build())
        .toList();

    accommodationImageRepository.saveAll(accommodationImages);
  }

  private Host validateHost(Long hostId) {
    Host host = getHost(hostId);
    validateNotExistsAccommodation(hostId);

    return host;
  }

  private List<MultipartFile> initAdditionalImages(List<MultipartFile> additionalImages) {
    return Optional.ofNullable(additionalImages)
        .orElseGet(Collections::emptyList);
  }

  private void validateNotExistsAccommodation(Long hostId) {
    if (accommodationRepository.existsByHostId(hostId)) {
      throw new MeongnyangerangException(ErrorCode.ACCOMMODATION_ALREADY_EXISTS);
    }
  }

  private Host getHost(Long hostId) {
    return hostRepository.findById(hostId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.NOT_EXISTS_HOST));
  }
}
