package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ACCOMMODATION_ALREADY_EXISTS;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ACCOMMODATION_NOT_FOUND;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXISTS_HOST;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationImage;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AllowPet;
import com.meongnyangerang.meongnyangerang.domain.accommodation.PetType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacility;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationFacilityType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacility;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacilityType;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.review.Review;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationCreateRequest;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationDetailResponse;
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

  private static final int MAX_ADDITIONAL_IMAGE_COUNT = 3;

  /**
   * 숙소 등록
   */
  public void createAccommodation(
      Long hostId,
      AccommodationCreateRequest request,
      MultipartFile thumbnail,
      List<MultipartFile> additionalImages
  ) {
    log.info("숙소 등록 시작");

    Host host = validateHost(hostId);
    additionalImages = initAdditionalImages(additionalImages);
    validateImagesCount(additionalImages.size());

    String thumbnailUrl = imageService.storeImage(thumbnail);
    List<String> newAdditionalImageUrls = uploadImages(additionalImages);

    Accommodation accommodation = request.toEntity(host, thumbnailUrl);
    saveAccommodationWithRelatedEntities(request, accommodation, newAdditionalImageUrls);

    log.info("숙소 등록 성공, 호스트 ID : {}, 숙소 ID : {}", hostId, accommodation.getId());
  }

  /**
   * 숙소 조회
   */
  public AccommodationResponse getAccommodation(Long hostId) {
    log.info("숙소 조회 시작");

    Accommodation accommodation = findAccommodationByHostId(hostId);
    Long accommodationId = accommodation.getId();

    List<AccommodationFacility> facilities = accommodationFacilityRepository
        .findAllByAccommodationId(accommodationId);

    List<AccommodationPetFacility> petFacilities = accommodationPetFacilityRepository
        .findAllByAccommodationId(accommodationId);

    List<AllowPet> allowPets = allowPetRepository.findAllByAccommodationId(accommodationId);
    List<String> additionalImageUrls = getImageUrls(accommodationId);

    log.info("숙소 조회 성공, 호스트 ID : {}, 숙소 ID : {}", hostId, accommodation.getId());
    return createAccommodationResponse(
        accommodation,
        facilities,
        petFacilities,
        allowPets,
        additionalImageUrls
    );
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
    log.info("숙소 수정 시작");
    
    Accommodation accommodation = findAccommodationByHostId(hostId);
    newAdditionalImages = initAdditionalImages(newAdditionalImages);
    validateImagesCount(newAdditionalImages.size());

    String newThumbnailUrl = thumbnailUpdate(accommodation.getThumbnailUrl(), thumbnail);
    List<String> newAdditionalImageUrls = additionalImagesUpdate(
        accommodation, newAdditionalImages, request.deleteImageUrls());

    accommodation.updateAccommodation(request, newThumbnailUrl);
    List<AccommodationFacility> updatedFacilities = updateFacilities(
        request.facilityTypes(), accommodation);
    List<AccommodationPetFacility> updatedPetFacilities = updatePetFacilities(
        request.petFacilityTypes(), accommodation);
    List<AllowPet> updatedAllowPets = updateAllowPets(request.allowPetTypes(), accommodation);

    updateSearchIndex(accommodation); // 색인 갱신 - 객실이 있을 경우에만

    log.info("숙소 수정 성공, 호스트 ID : {}, 숙소 ID : {}", hostId, accommodation.getId());
    return createAccommodationResponse(
        accommodation,
        updatedFacilities,
        updatedPetFacilities,
        updatedAllowPets,
        newAdditionalImageUrls
    );
  }

  /**
   * 숙소 상세 조회(비로그인 사용자, 일반 사용자, 호스트 모두 접근 가능한 API)
   */
  @Transactional
  public AccommodationDetailResponse getAccommodationDetail(Long accommodationId) {
    Accommodation accommodation = accommodationRepository.findById(accommodationId)
        .orElseThrow(() -> new MeongnyangerangException(ACCOMMODATION_NOT_FOUND));

    // 숙소 이미지
    List<AccommodationImage> images = accommodationImageRepository.findAllByAccommodationId(
        accommodationId);

    // 숙소 시설
    List<AccommodationFacility> facilities = accommodationFacilityRepository.findAllByAccommodationId(
        accommodationId);

    // 반려동물 시설
    List<AccommodationPetFacility> petFacilities = accommodationPetFacilityRepository.findAllByAccommodationId(
        accommodationId);

    // 허용 반려동물
    List<AllowPet> allowPets = allowPetRepository.findAllByAccommodationId(accommodationId);

    // 객실 목록 (가격 오름차순)
    List<Room> rooms = roomRepository.findAllByAccommodationIdOrderByPriceAsc(accommodationId);

    // 최신 리뷰 5개
    List<Review> reviews = reviewRepository.findTop5ByAccommodationIdOrderByCreatedAtDesc(
        accommodationId);

    accommodationRepository.incrementViewCount(accommodationId);

    return AccommodationDetailResponse.of(accommodation, images, facilities, petFacilities,
        allowPets, reviews, rooms);
  }

  private void saveAccommodationWithRelatedEntities(
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

  private String thumbnailUpdate(String oldThumbnailUrl, MultipartFile newThumbnail) {
    if (newThumbnail == null || newThumbnail.isEmpty()) {
      return oldThumbnailUrl;
    }
    imageService.deleteImageAsync(oldThumbnailUrl);

    return imageService.storeImage(newThumbnail);
  }

  private List<String> additionalImagesUpdate(
      Accommodation accommodation,
      List<MultipartFile> newAdditionalImages,
      List<String> deleteImageUrls
  ) {
    // 현재 숙소에 연결된 모든 추가 이미지 개수를 가져옴
    int currentImagesCount = accommodationImageRepository.countByAccommodationId(
        accommodation.getId());

    // 삭제될 이미지 개수 계산
    int deleteCount = (deleteImageUrls != null) ? deleteImageUrls.size() : 0;

    // 삭제 후 남을 이미지 개수 + 새로 추가될 이미지 개수가 3을 초과하는지 확인
    int totalImagesAfterUpdate = currentImagesCount - deleteCount + newAdditionalImages.size();
    validateImagesCount(totalImagesAfterUpdate);
    
    if (deleteCount > 0) {
      imageService.deleteImagesAsync(deleteImageUrls);
      accommodationImageRepository.deleteAllByImageUrl(deleteImageUrls);
    }
    List<String> newAdditionalImageUrls = uploadImages(newAdditionalImages);
    saveAdditionalImages(newAdditionalImageUrls, accommodation);

    return newAdditionalImageUrls;
  }

  private List<String> getImageUrls(Long accommodationId) {
    return accommodationImageRepository
        .findAllByAccommodationId(accommodationId)
        .stream()
        .map(AccommodationImage::getImageUrl)
        .toList();
  }

  private AccommodationResponse createAccommodationResponse(
      Accommodation accommodation,
      List<AccommodationFacility> savedFacility,
      List<AccommodationPetFacility> savedPetFacilities,
      List<AllowPet> savedAllowPets,
      List<String> additionalImageUrls
  ) {
    return AccommodationResponse.of(
        accommodation,
        savedFacility,
        savedPetFacilities,
        savedAllowPets,
        additionalImageUrls
    );
  }

  private Accommodation findAccommodationByHostId(Long hostId) {
    return accommodationRepository.findByHostId(hostId)
        .orElseThrow(() -> new MeongnyangerangException(ACCOMMODATION_NOT_FOUND));
  }

  private List<AccommodationFacility> updateFacilities(
      List<AccommodationFacilityType> newFacilityTypes, Accommodation accommodation
  ) {
    accommodationFacilityRepository.deleteAllByAccommodationId(accommodation.getId());
    return saveAccommodationFacilities(newFacilityTypes, accommodation);
  }

  private List<AccommodationPetFacility> updatePetFacilities(
      List<AccommodationPetFacilityType> newPetFacilityTypes, Accommodation accommodation
  ) {
    accommodationPetFacilityRepository.deleteAllByAccommodationId(accommodation.getId());
    return saveAccommodationPetFacilities(newPetFacilityTypes, accommodation);
  }

  private List<AllowPet> updateAllowPets(List<PetType> newPetTypes, Accommodation accommodation) {
    allowPetRepository.deleteAllByAccommodationId(accommodation.getId());
    return saveAllowPets(newPetTypes, accommodation);
  }

  private List<String> uploadImages(List<MultipartFile> additionalImages) {
    return additionalImages.stream()
        .map(imageService::storeImage)
        .toList();
  }

  private List<AccommodationFacility> saveAccommodationFacilities(
      List<AccommodationFacilityType> facilityTypes, Accommodation accommodation
  ) {
    List<AccommodationFacility> facilities = facilityTypes.stream()
        .map(facilityType -> AccommodationFacility.builder()
            .accommodation(accommodation)
            .type(facilityType)
            .build())
        .toList();

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
        .toList();

    return accommodationPetFacilityRepository.saveAll(petFacilities);
  }

  private List<AllowPet> saveAllowPets(List<PetType> petTypes, Accommodation accommodation) {
    List<AllowPet> allowPets = petTypes.stream()
        .map(petType -> AllowPet.builder()
            .accommodation(accommodation)
            .petType(petType)
            .build())
        .toList();

    return allowPetRepository.saveAll(allowPets);
  }

  private void saveAdditionalImages(List<String> additionalImageUrls, Accommodation accommodation) {
    List<AccommodationImage> accommodationImages = additionalImageUrls.stream()
        .map(additionalImageUrl -> AccommodationImage.builder()
            .accommodation(accommodation)
            .imageUrl(additionalImageUrl)
            .build())
        .toList();

    accommodationImageRepository.saveAll(accommodationImages);
  }

  private void updateSearchIndex(Accommodation accommodation) {
    List<Room> rooms = roomRepository.findAllByAccommodationId(accommodation.getId());
    searchService.updateAllRooms(accommodation, rooms);
    searchService.updateAccommodationDocument(accommodation);
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

  private void validateImagesCount(int imageSize) {
    if (imageSize > MAX_ADDITIONAL_IMAGE_COUNT) {
      throw new MeongnyangerangException(ErrorCode.MAX_IMAGE_LIMIT_EXCEEDED);
    }
  }
}
