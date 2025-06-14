package com.meongnyangerang.meongnyangerang.service.recommendation;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationDocument;
import com.meongnyangerang.meongnyangerang.domain.accommodation.PetType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacilityType;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacilityType;
import com.meongnyangerang.meongnyangerang.domain.user.UserPet;
import com.meongnyangerang.meongnyangerang.dto.accommodation.PetRecommendationGroup;
import com.meongnyangerang.meongnyangerang.dto.accommodation.RecommendationResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.UserPetRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomRepository;
import com.meongnyangerang.meongnyangerang.service.WishlistService;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccommodationRecommendationService {

  private final ElasticsearchClient elasticsearchClient;
  private final UserPetRepository userPetRepository;
  private final AccommodationRepository accommodationRepository;
  private final RoomRepository roomRepository;
  private final WishlistService wishlistService;
  private final PetFacilityScoreService petFacilityScoreService;

  private static final String INDEX_NAME = "accommodations";
  private static final int SIZE = 6;
  private static final int MAX_RESULTS = 100;

  // 비로그인 사용자 기본 추천
  public Map<String, List<RecommendationResponse>> getDefaultRecommendations() {
    Map<String, List<RecommendationResponse>> result = new HashMap<>();

    for (PetType petType : PetType.values()) {
      result.put(petType.getValue(), searchByPetType(petType));
    }

    return result;
  }

  // 비로그인 사용자 기본 추천 더보기
  public PageResponse<RecommendationResponse> getDefaultLoadMoreRecommendations(PetType type,
      Pageable pageable) {
    int size = calculateActualSize(pageable);
    int from = calculateFromOffset(pageable);

    Query query = buildPetTypeQuery(type.name());

    // Elasticsearch 요청
    SearchRequest request = buildSearchRequest(query, "totalRating", true, size, from);

    try {
      // Elasticsearch에 요청 후 데이터 반환
      SearchResponse<AccommodationDocument> response =
          elasticsearchClient.search(request, AccommodationDocument.class);

      List<RecommendationResponse> content = response.hits().hits().stream()
          .map(Hit::source).filter(Objects::nonNull)
          .map(RecommendationResponse::from)
          .toList();

      // 총 개수 및 페이지 계산
      long totalElements = response.hits().total().value();

      return buildPageResponse(content, pageable, size, totalElements, from);
    } catch (IOException e) {
      throw new MeongnyangerangException(ErrorCode.DEFAULT_RECOMMENDATION_FAILED);
    }
  }

  // 사용자가 등록한 반려동물 기반 추천
  public List<PetRecommendationGroup> getUserPetRecommendations(Long userId) {
    List<UserPet> userPets = userPetRepository.findAllByUserId(userId);
    Set<Long> wishlistedIds = wishlistService.getWishlistIdsFromRedis(userId);

    return userPets.stream()
        .map(pet -> new PetRecommendationGroup(
            pet.getId(),
            pet.getName(),
            searchByUserPet(pet, wishlistedIds)))
        .toList();
  }

  // 사용자가 등록한 반려동물 기반 추천 더보기
  public PageResponse<RecommendationResponse> getUserPetLoadMoreRecommendations(Long userId,
      Long petId, Pageable pageable) {
    UserPet pet = validateAndGetUserPet(userId, petId);

    int size = calculateActualSize(pageable);
    int from = calculateFromOffset(pageable);

    Map<String, Integer> accScoreMap = getAccommodationScoreMap(pet);
    Map<String, Integer> roomScoreMap = getRoomScoreMap(pet);
    Set<Long> wishlistedIds = wishlistService.getWishlistIdsFromRedis(userId);

    // 해당 반려동물 유형 필터링 쿼리
    Query query = buildPetTypeQuery(pet.getType().name());

    // Elasticsearch 요청 생성 (정렬 없이 점수 후처리용)
    SearchRequest request = buildSearchRequest(query, "totalRating", false, size, from);

    try {
      SearchResponse<AccommodationDocument> response =
          elasticsearchClient.search(request, AccommodationDocument.class);

      // 점수 계산 및 정렬
      List<RecommendationResponse> content = calculateScoreAndSort(response, accScoreMap,
          roomScoreMap, wishlistedIds);

      long totalElements = response.hits().total().value();

      return buildPageResponse(content, pageable, size, totalElements, from);
    } catch (IOException e) {
      throw new MeongnyangerangException(ErrorCode.USER_RECOMMENDATION_FAILED);
    }
  }

  // 많은 사람들이 관심을 가진 숙소 추천
  public List<RecommendationResponse> getMostViewedRecommendations(Long userId) {
    List<Accommodation> accommodations = accommodationRepository.findTop10ByOrderByViewCountDescTotalRatingDesc();
    Set<Long> wishlistedIds = wishlistService.getWishlistIdsFromRedis(userId);

    return accommodations.stream()
        .map(accommodation -> {
          Room room = roomRepository
              .findFirstByAccommodationOrderByPriceAsc(accommodation);

          return RecommendationResponse.builder()
              .id(accommodation.getId())
              .name(accommodation.getName())
              .price(room.getPrice())
              .totalRating(accommodation.getTotalRating())
              .thumbnailUrl(accommodation.getThumbnailUrl())
              .isWishlisted(wishlistedIds.contains(accommodation.getId()))
              .build();
        })
        .toList();
  }

  // PetType에 기반한 추천 숙소 검색 (비로그인 사용자용)
  private List<RecommendationResponse> searchByPetType(PetType petType) {
    // allowedPetTypes 필드에 해당 petType이 포함된 문서를 검색하는 쿼리 생성
    Query query = buildPetTypeQuery(petType.name());

    // totalRating 기준으로 내림차순 정렬된 검색 요청 생성 (source 필드 필터 포함)
    SearchRequest request = buildSearchRequest(query, "totalRating", true, 0, 0);

    try {
      // Elasticsearch에 요청 후 RecommendationResponse로 매핑된 결과 반환
      SearchResponse<RecommendationResponse> response =
          elasticsearchClient.search(request, RecommendationResponse.class);

      // 검색 결과에서 source만 추출하여 리스트로 반환
      return response.hits().hits().stream()
          .map(Hit::source)
          .toList();
    } catch (IOException e) {
      throw new MeongnyangerangException(ErrorCode.DEFAULT_RECOMMENDATION_FAILED);
    }
  }

  // 사용자 반려동물 정보를 바탕으로 맞춤 추천 수행
  private List<RecommendationResponse> searchByUserPet(UserPet pet, Set<Long> wishlistedIds) {
    // 반려동물 성향에 따른 시설 점수 맵 생성
    Map<String, Integer> accScoreMap = getAccommodationScoreMap(pet);
    Map<String, Integer> roomScoreMap = getRoomScoreMap(pet);

    // allowedPetTypes 필드 기반 필터링 쿼리 생성
    Query query = buildPetTypeQuery(pet.getType().name());

    // 정렬 없이 (점수로 후처리 예정) 검색 요청 생성 (source 필터링 생략)
    SearchRequest request = buildSearchRequest(query, null, false, 0, 0);

    try {
      // Elasticsearch에 요청 후 AccommodationDocument로 매핑된 결과 반환
      SearchResponse<AccommodationDocument> response =
          elasticsearchClient.search(request, AccommodationDocument.class);

      // 각 문서에 대해 점수 계산 후 정렬하여 RecommendationResponse로 변환
      return calculateScoreAndSort(response, accScoreMap, roomScoreMap, wishlistedIds);
    } catch (IOException e) {
      throw new MeongnyangerangException(ErrorCode.USER_RECOMMENDATION_FAILED);
    }
  }

  // allowedPetTypes에 정확히 일치하는 petType을 검색하는 쿼리 생성
  private Query buildPetTypeQuery(String petType) {
    return TermQuery.of(t -> t
        .field("allowedPetTypes.keyword")
        .value(petType)
    )._toQuery();
  }

  // 공통 SearchRequest 생성 메서드
  private SearchRequest buildSearchRequest(Query query, String sortField,
      boolean withSourceFilter, int size, int from) {
    return SearchRequest.of(s -> {
      // 기본 검색 조건 설정: 인덱스, 쿼리, 결과 크기
      SearchRequest.Builder builder = s.index(INDEX_NAME)
          .query(query);

      if (size != 0) {
        builder.size(size).from(from);
      } else {
        builder.size(SIZE);
      }

      // 정렬 필드가 존재할 경우 정렬 추가
      if (sortField != null) {
        builder = builder.sort(sort -> sort
            .field(f -> f.field(sortField).order(SortOrder.Desc)));
      }

      // source 필드 필터링 옵션이 활성화된 경우, 필요한 필드만 포함
      if (withSourceFilter) {
        builder = builder.source(src -> src.filter(
            f -> f.includes("id", "name", "price", "totalRating", "thumbnailUrl")));
      }

      return builder;
    });
  }

  private PageResponse<RecommendationResponse> buildPageResponse(
      List<RecommendationResponse> content, Pageable pageable,
      int size, long totalElements, int from) {

    int totalPages = (int) Math.ceil((double) totalElements / size);
    boolean isFirst = pageable.getPageNumber() == 0;
    boolean isLast = from + size >= MAX_RESULTS || totalElements <= (from + size);

    return new PageResponse<>(
        content,
        pageable.getPageNumber(),
        size,
        totalElements,
        totalPages,
        isFirst,
        isLast
    );
  }

  // 반려동물 정보로 숙소 시설 점수 맵 생성
  private Map<String, Integer> getAccommodationScoreMap(UserPet pet) {
    return petFacilityScoreService.getAccommodationScore(pet.getType().name(),
        pet.getActivityLevel().name(), pet.getPersonality().name());
  }

  // 반려동물 정보로 객실 시설 점수 맵 생성
  private Map<String, Integer> getRoomScoreMap(UserPet pet) {
    return petFacilityScoreService.getRoomScore(pet.getType().name(), pet.getActivityLevel().name(),
        pet.getPersonality().name());
  }

  // 숙소의 시설 점수를 계산 (반려동물 성향 기반)
  private int calculateScore(AccommodationDocument doc,
      Map<String, Integer> accMap,
      Map<String, Integer> roomMap) {

    // 숙소 시설 점수 계산
    int accScore = doc.getAccommodationPetFacilities().stream()
        .map(AccommodationPetFacilityType::valueOf)
        .mapToInt(facility -> accMap.getOrDefault(facility.name(), 0))
        .sum();

    // 객실 시설 점수 계산
    int roomScore = doc.getRoomPetFacilities().stream()
        .map(RoomPetFacilityType::valueOf)
        .mapToInt(facility -> roomMap.getOrDefault(facility.name(), 0))
        .sum();

    // 총점 반환
    return accScore + roomScore;
  }

  private int calculateActualSize(Pageable pageable) {
    int size = pageable.getPageSize();
    int from = calculateFromOffset(pageable);

    if (from + size > MAX_RESULTS) {
      return Math.max(0, MAX_RESULTS - from);
    }
    return size;
  }

  private int calculateFromOffset(Pageable pageable) {
    return pageable.getPageNumber() == 0 ? SIZE : (int) pageable.getOffset();
  }

  // 점수 계산 및 정렬 처리
  private List<RecommendationResponse> calculateScoreAndSort(
      SearchResponse<AccommodationDocument> response,
      Map<String, Integer> accScoreMap,
      Map<String, Integer> roomScoreMap,
      Set<Long> wishlistedIds) {

    return response.hits().hits().stream()
        .map(Hit::source)
        .filter(Objects::nonNull)
        .map(doc -> new AbstractMap.SimpleEntry<>(calculateScore(doc, accScoreMap, roomScoreMap),
            doc))
        .sorted((a, b) -> Integer.compare(b.getKey(), a.getKey()))
        .map(entry -> RecommendationResponse.from(entry.getValue(), wishlistedIds))
        .toList();
  }

  private UserPet validateAndGetUserPet(Long userId, Long petId) {
    Optional<UserPet> petOptional = userPetRepository.findById(petId);

    if (petOptional.isEmpty()) {
      throw new MeongnyangerangException(ErrorCode.NOT_EXIST_PET);
    }

    UserPet pet = petOptional.get();
    if (!Objects.equals(pet.getUser().getId(), userId)) {
      throw new MeongnyangerangException(ErrorCode.INVALID_AUTHORIZED);
    }

    return pet;
  }
}
