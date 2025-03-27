package com.meongnyangerang.meongnyangerang.service;

import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
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
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationPetFacilityRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AllowPetRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccommodationService {

  private final AccommodationRepository accommodationRepository;
  private final AccommodationFacilityRepository accommodationFacilityRepository;
  private final AccommodationPetFacilityRepository accommodationPetFacilityRepository;
  private final AllowPetRepository allowPetRepository;
  //private final ImageService imageService;

  /**
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
    Host host = validateHost(hostId); // 호스트 검증

    // TODO: 이미지 파일 업로드 시 마인타입과 확장자 검증 필요
    // S3 다중 이미지 파일 업로드
    additionalImages.add(thumbnail);
    
    // 썸네일 + 추가 이미지 한번에 업로드
    List<String> additionalImageUrls = List.of(); // = imageService.storeImageFiles(images);

    // 숙소 엔티티 생성
    Accommodation accommodation = request.toEntity(host, additionalImageUrls.get(0));

    // 숙소 저장
    accommodationRepository.save(accommodation);

    // TODO: additionalImageUrls이 size() > 1일 시, DB에 저장하는 코드 추가

    // 편의시설 정보 저장
    saveAccommodationFacilities(accommodation, request.getFacilities());

    // 반려동물 편의시설 정보 저장
    saveAccommodationPetFacilities(accommodation, request.getPetFacilities());

    // 허용 반려동물 정보 저장
    saveAllowedPets(accommodation, request.getAllowedPets());
  }

  private Host validateHost(Long hostId) {
    Host host = null; // TODO: 호스트 존재하는지 검증 400

    if (host.getStatus() != HostStatus.ACTIVE) {
      throw new RuntimeException(); // TODO: CustomException 으로 변경 403
    }

    // 호스트 숙소 있는지 검증
    if (accommodationRepository.existsByHost_Id(hostId)) {
      throw new RuntimeException(); // TODO: CustomException 으로 변경 409
    }
    return host;
  }

  /**
   * 숙소 편의시설 저장
   */
  private void saveAccommodationFacilities(
      Accommodation accommodation, List<String> facilityTypes
  ) {
    if (facilityTypes != null && !facilityTypes.isEmpty()) {
      for (String facilityType : facilityTypes) {
        try {
          AccommodationFacilityType type =
              AccommodationFacilityType.valueOf(facilityType.toUpperCase());

          AccommodationFacility facility = AccommodationFacility.builder()
              .accommodation(accommodation)
              .type(type)
              .build();

          accommodationFacilityRepository.save(facility);
        } catch (IllegalArgumentException e) {
          log.warn("Invalid facility type: {}", facilityType);
          throw new RuntimeException(); // TODO: CustomException 으로 변경
        }
      }
    }
  }

  /**
   * 숙소 반려동물 편의시설 저장
   */
  private void saveAccommodationPetFacilities(
      Accommodation accommodation, List<String> petFacilityTypes
  ) {
    if (petFacilityTypes != null && !petFacilityTypes.isEmpty()) {
      for (String petFacilityType : petFacilityTypes) {
        try {
          AccommodationPetFacilityType type =
              AccommodationPetFacilityType.valueOf(petFacilityType.toUpperCase());

          AccommodationPetFacility petFacility = AccommodationPetFacility.builder()
              .accommodation(accommodation)
              .type(type)
              .build();

          accommodationPetFacilityRepository.save(petFacility);
        } catch (IllegalArgumentException e) {
          log.warn("Invalid pet facility type: {}", petFacilityType);
          throw new RuntimeException(); // TODO: CustomException 으로 변경
        }
      }
    }
  }

  /**
   * 허용 반려동물 정보 저장
   */
  private void saveAllowedPets(Accommodation accommodation, List<String> petTypes) {
    if (petTypes != null && !petTypes.isEmpty()) {
      for (String petType : petTypes) {
        try {
          PetType type = PetType.valueOf(petType.toUpperCase());

          AllowPet allowPet = AllowPet.builder()
              .accommodation(accommodation)
              .petType(type)
              .build();

          allowPetRepository.save(allowPet);
        } catch (IllegalArgumentException e) {
          log.warn("Invalid pet type: {}", petType);
          throw new RuntimeException(); // TODO: CustomException 으로 변경
        }
      }
    }
  }
}
