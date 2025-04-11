package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.*;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationImage;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AllowPet;
import com.meongnyangerang.meongnyangerang.domain.accommodation.PetType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacility;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacilityType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacility;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacilityType;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationCreateRequest;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationDetailResponse;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationDetailResponse.ReviewSummary;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationDetailResponse.RoomDetail;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationResponse;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationUpdateRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.ReviewRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationImageRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationPetFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AllowPetRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
  private final RoomRepository roomRepository;
  private final AccommodationRoomSearchService searchService;
  private final ReviewRepository reviewRepository;

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
      imageService.deleteImagesAsync(trackingList);
      throw new MeongnyangerangException(ACCOMMODATION_REGISTRATION_FAILED);
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
      imageService.deleteImagesAsync(oldImageUrlsToDelete);

      // 색인 갱신 - 객실이 있을 경우에만
      List<Room> rooms = roomRepository.findAllByAccommodationId(accommodation.getId());
      searchService.updateAllRooms(accommodation, rooms);

      log.info("숙소 수정 성공, 숙소 ID : {}", accommodation.getId());
      return createAccommodationResponse(accommodation);
    } catch (Exception e) {
      log.error("숙소 수정 에러 발생, S3에 업로드된 이미지 삭제 처리, 원인: {}", e.getMessage());
      imageService.deleteImagesAsync(trackingList);
      throw new MeongnyangerangException(ACCOMMODATION_REGISTRATION_FAILED);
    }
  }

  /**
   * 숙소 상세 조회(비로그인 사용자, 일반 사용자, 호스트 모두 접근 가능한 API)
   */
  @Transactional(readOnly = true)
  public AccommodationDetailResponse getAccommodationDetail(Long accommodationId) {
    Accommodation accommodation = accommodationRepository.findById(accommodationId)
        .orElseThrow(() -> new MeongnyangerangException(ACCOMMODATION_NOT_FOUND));

    // 숙소 이미지
    List<String> imageUrls = accommodationImageRepository.findAllByAccommodationId(accommodationId)
        .stream().map(AccommodationImage::getImageUrl).toList();

    // 숙소 시설
    List<String> facilityList = accommodationFacilityRepository.findAllByAccommodationId(accommodationId)
        .stream().map(f -> f.getType().getValue()).toList();

    // 반려동물 시설
    List<String> petFacilityList = accommodationPetFacilityRepository.findAllByAccommodationId(accommodationId)
        .stream().map(f -> f.getType().getValue()).toList();

    // 허용 반려동물
    List<String> allowedPets = allowPetRepository.findAllByAccommodationId(accommodationId)
        .stream().map(a -> a.getPetType().getValue()).toList();

    // 객실 목록 (가격 오름차순)
    List<RoomDetail> roomDetails = roomRepository.findAllByAccommodationIdOrderByPriceAsc(accommodationId)
        .stream().map(this::toRoomDetail).toList();

    // 최신 리뷰 5개
    List<ReviewSummary> reviewSummaries = reviewRepository.findTop5ByAccommodationIdOrderByCreatedAtDesc(accommodationId)
        .stream().map(this::toReviewSummary).toList();

    return AccommodationDetailResponse.builder()
        .accommodationId(accommodation.getId())
        .name(accommodation.getName())
        .description(accommodation.getDescription())
        .address(accommodation.getAddress())
        .detailedAddress(accommodation.getDetailedAddress())
        .type(accommodation.getType().name())
        .thumbnailUrl(accommodation.getThumbnailUrl())
        .accommodationImages(imageUrls)
        .totalRating(accommodation.getTotalRating())
        .accommodationFacility(facilityList)
        .accommodationPetFacility(petFacilityList)
        .allowPet(allowedPets)
        .latitude(accommodation.getLatitude())
        .longitude(accommodation.getLongitude())
        .review(reviewSummaries)
        .rooms(roomDetails)
        .build();
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
        .orElseThrow(() -> new MeongnyangerangException(ACCOMMODATION_NOT_FOUND));
  }

  private void updateEntities(
      Accommodation accommodation,
      AccommodationUpdateRequest request,
      String newThumbnailUrl,
      List<String> newAdditionalImageUrls
  ) {
    accommodation.updateAccommodation(request, newThumbnailUrl);
    updateFacilities(request.facilityTypes(), accommodation);

    updatePetFacilities(request.petFacilityTypes(), accommodation);
    updateAllowPets(request.allowPetTypes(), accommodation);

    if (!newAdditionalImageUrls.isEmpty()) {
      accommodationImageRepository.deleteAllByAccommodationId(accommodation.getId());
      saveAdditionalImages(newAdditionalImageUrls, accommodation);
    }
  }

  private void updateFacilities(
      List<AccommodationFacilityType> newFacilityTypes, Accommodation accommodation
  ) {
    accommodationFacilityRepository.deleteAllByAccommodationId(accommodation.getId());
    saveAccommodationFacilities(newFacilityTypes, accommodation);
  }

  private void updatePetFacilities(
      List<AccommodationPetFacilityType> newPetFacilityTypes, Accommodation accommodation
  ) {
    accommodationPetFacilityRepository.deleteAllByAccommodationId(accommodation.getId());
    saveAccommodationPetFacilities(newPetFacilityTypes, accommodation);
  }

  private void updateAllowPets(List<PetType> newPetTypes, Accommodation accommodation) {
    allowPetRepository.deleteAllByAccommodationId(accommodation.getId());
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
        .toList();

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
        .toList();

    accommodationPetFacilityRepository.saveAll(petFacilities);
  }

  private void saveAllowPets(List<PetType> petTypes, Accommodation accommodation) {
    List<AllowPet> allowPets = petTypes.stream()
        .map(petType -> AllowPet.builder()
            .accommodation(accommodation)
            .petType(petType)
            .build())
        .toList();

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
      throw new MeongnyangerangException(ACCOMMODATION_ALREADY_EXISTS);
    }
  }

  private Host getHost(Long hostId) {
    return hostRepository.findById(hostId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXISTS_HOST));
  }
}
