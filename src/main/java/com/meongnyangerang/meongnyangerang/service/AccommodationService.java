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

    List<String> uploadedImageUrls = new ArrayList<>(); // 업로드 성공한 이미지 추적

    try {
      String thumbnailUrl = imageService.storeImage(thumbnail);
      uploadedImageUrls.add(thumbnailUrl);
      List<String> imageUrls = storeImages(additionalImages, uploadedImageUrls);

      Accommodation accommodation = request.toEntity(host, thumbnailUrl);
      saveData(request, accommodation, imageUrls);

      log.info("숙소 등록 성공, 호스트 ID : {}, 숙소 ID : {}", hostId, accommodation.getId());
    } catch (MeongnyangerangException e) {
      imageService.deleteImages(uploadedImageUrls);
      throw e;
    } catch (Exception e) {
      log.error("숙소 등록 에러 발생, S3에 업로드된 이미지 삭제, 원인: {}", e.getMessage());
      imageService.deleteImages(uploadedImageUrls);
      throw new MeongnyangerangException(ErrorCode.REGISTRATION_ACCOMMODATION);
    }
  }

  @Transactional
  public void saveData(
      AccommodationCreateRequest request,
      Accommodation accommodation,
      List<String> imageUrls
  ) {
    accommodationRepository.save(accommodation);
    saveAccommodationFacilities(request.getFacilityTypes(), accommodation);
    saveAccommodationPetFacilities(request.getPetFacilityTypes(), accommodation);
    saveAllowPets(request.getAllowPetTypes(), accommodation);
    saveAdditionalImages(imageUrls, accommodation);
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

  /**
   * 숙소 수정
   */
  @Transactional
  public AccommodationResponse updateAccommodation(
      AccommodationUpdateRequest request,
      MultipartFile thumbnail,
      List<MultipartFile> additionalImages
  ) {
    additionalImages = initAdditionalImages(additionalImages);

    List<String> uploadedImageUrls = new ArrayList<>(); // 업로드 성공한 이미지 추적

    try {
      String thumbnailUrl = imageService.storeImage(thumbnail);
      uploadedImageUrls.add(thumbnailUrl);
      List<String> imageUrls = storeImages(additionalImages, uploadedImageUrls);

      AccommodationResponse accommodationResponse = updateData(request, thumbnailUrl, imageUrls);

      List<String> oldImages =
          imagesDeleted(request.oldThumbnailUrl(), request.oldAdditionalImageUrls());
      imageService.registerImagesForDeletion(oldImages);

      log.info("숙소 수정 성공, 숙소 ID : {}", request.accommodationId());
      return accommodationResponse;

    } catch (MeongnyangerangException e) {
      imageService.deleteImages(uploadedImageUrls);
      throw e;
    } catch (Exception e) {
      log.error("숙소 수정 에러 발생, S3에 업로드된 이미지 삭제 처리, 원인: {}", e.getMessage());
      imageService.deleteImages(uploadedImageUrls);
      throw new MeongnyangerangException(ErrorCode.REGISTRATION_ACCOMMODATION);
    }
  }

  private AccommodationResponse updateData(
      AccommodationUpdateRequest request,
      String thumbnailUrl,
      List<String> imageUrls
  ) {
    Long accommodationId = request.accommodationId();

    Accommodation accommodation = accommodationRepository.findById(accommodationId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.ACCOMMODATION_NOT_FOUND));
    accommodation.updateAccommodation(request, thumbnailUrl);

    accommodationFacilityRepository.deleteAllByAccommodationId(accommodationId);
    List<AccommodationFacility> facilities =
        saveAccommodationFacilities(request.facilityTypes(), accommodation);

    accommodationPetFacilityRepository.deleteAllByAccommodationId(accommodationId);
    List<AccommodationPetFacility> petFacilities =
        saveAccommodationPetFacilities(request.petFacilityTypes(), accommodation);

    allowPetRepository.deleteAllByAccommodationId(accommodationId);
    List<AllowPet> allowPets = saveAllowPets(request.allowPetTypes(), accommodation);

    accommodationImageRepository.deleteAllByAccommodationId(accommodationId);
    List<AccommodationImage> accommodationImages = saveAdditionalImages(imageUrls, accommodation);

    return AccommodationResponse.of(
        accommodation,
        facilities,
        petFacilities,
        allowPets,
        accommodationImages
    );
  }

  private List<String> storeImages(
      List<MultipartFile> additionalImages,
      List<String> uploadedImageUrls
  ) {
    List<String> imageUrls = new ArrayList<>();

    for (MultipartFile image : additionalImages) {
      String imageUrl = imageService.storeImage(image);
      imageUrls.add(imageUrl);
      uploadedImageUrls.add(imageUrl);
    }

    return imageUrls;
  }

  private List<AccommodationFacility> saveAccommodationFacilities(
      List<AccommodationFacilityType> facilityTypes, Accommodation accommodation
  ) {
    List<AccommodationFacility> facilities = facilityTypes.stream()
        .map(facilityType -> AccommodationFacility.builder()
            .accommodation(accommodation)
            .type(facilityType)
            .build())
        .collect(Collectors.toList());

    return accommodationFacilityRepository.saveAll(facilities);
  }

  private List<AccommodationPetFacility> saveAccommodationPetFacilities(
      List<AccommodationPetFacilityType> petFacilityTypes, Accommodation accommodation
  ) {
    List<AccommodationPetFacility> petFacilities = petFacilityTypes.stream()
        .map(petFacilityType -> AccommodationPetFacility.builder()
            .accommodation(accommodation)
            .type(petFacilityType)
            .build())
        .collect(Collectors.toList());

    return accommodationPetFacilityRepository.saveAll(petFacilities);
  }

  private List<AllowPet> saveAllowPets(List<PetType> petTypes, Accommodation accommodation) {
    List<AllowPet> allowPets = petTypes.stream()
        .map(petType -> AllowPet.builder()
            .accommodation(accommodation)
            .petType(petType)
            .build())
        .collect(Collectors.toList());

    return allowPetRepository.saveAll(allowPets);
  }

  private List<AccommodationImage> saveAdditionalImages(
      List<String> filenames, Accommodation accommodation
  ) {
    List<AccommodationImage> accommodationImages = filenames.stream()
        .map(filename -> AccommodationImage.builder()
            .accommodation(accommodation)
            .imageUrl(filename)
            .build())
        .toList();

    return accommodationImageRepository.saveAll(accommodationImages);
  }

  private List<MultipartFile> initAdditionalImages(List<MultipartFile> additionalImages) {
    return Optional.ofNullable(additionalImages)
        .orElseGet(Collections::emptyList);
  }

  private List<String> imagesDeleted(
      String oldThumbnailUrl, List<String> oldAdditionalImageUrls
  ) {
    List<String> oldImages = new ArrayList<>();
    oldImages.add(oldThumbnailUrl);
    oldImages.addAll(oldAdditionalImageUrls);
    return oldImages;
  }

  private Host validateHost(Long hostId) {
    Host host = getHost(hostId);
    validateNotExistsAccommodation(hostId);

    return host;
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
