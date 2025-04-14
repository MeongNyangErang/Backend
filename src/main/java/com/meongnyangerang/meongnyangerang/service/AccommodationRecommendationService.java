package com.meongnyangerang.meongnyangerang.service;

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
import com.meongnyangerang.meongnyangerang.domain.recommendation.PetFacilityScoreMap;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacilityType;
import com.meongnyangerang.meongnyangerang.domain.user.ActivityLevel;
import com.meongnyangerang.meongnyangerang.domain.user.Personality;
import com.meongnyangerang.meongnyangerang.domain.user.UserPet;
import com.meongnyangerang.meongnyangerang.dto.accommodation.RecommendationResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.UserPetRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomRepository;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccommodationRecommendationService {

  private final ElasticsearchClient elasticsearchClient;
  private final UserPetRepository userPetRepository;
  private final AccommodationRepository accommodationRepository;
  private final RoomRepository roomRepository;

  private static final String INDEX_NAME = "accommodations";
  private static final int SIZE = 6;

  // 비로그인 사용자 기본 추천
  public Map<String, List<RecommendationResponse>> getDefaultRecommendations() {
    Map<String, List<RecommendationResponse>> result = new HashMap<>();

    for (PetType petType : PetType.values()) {
      result.put(petType.name(), searchByPetType(petType));
    }

    return result;
  }

  // 로그인 사용자 맞춤 추천
  public Map<String, List<RecommendationResponse>> getUserPetRecommendations(Long userId) {
    List<UserPet> userPets = userPetRepository.findAllByUserId(userId);
    Map<String, List<RecommendationResponse>> result = new HashMap<>();
    for (UserPet pet : userPets) {
      result.put(pet.getName(), searchByUserPet(pet));
    }
    return result;
  }

  // 조회수 기반 인기 숙소 추천
  public List<RecommendationResponse> getPopularRecommendations() {
    List<Accommodation> accommodations = accommodationRepository.findTop10ByOrderByViewCountDesc();

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
              .build();
        })
        .toList();
  }

  // PetType에 기반한 추천 숙소 검색 (비로그인 사용자용)
  private List<RecommendationResponse> searchByPetType(PetType petType) {
    // allowedPetTypes 필드에 해당 petType이 포함된 문서를 검색하는 쿼리 생성
    Query query = buildPetTypeQuery(petType.name());

    // totalRating 기준으로 내림차순 정렬된 검색 요청 생성 (source 필드 필터 포함)
    SearchRequest request = buildSearchRequest(query, "totalRating", true);

    try {
      // Elasticsearch에 요청 후 RecommendationResponse로 매핑된 결과 반환
      SearchResponse<RecommendationResponse> response =
          elasticsearchClient.search(request, RecommendationResponse.class);

      // 검색 결과에서 source만 추출하여 리스트로 반환
      return response.hits().hits().stream()
          .map(Hit::source)
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new MeongnyangerangException(ErrorCode.DEFAULT_RECOMMENDATION_FAILED);
    }
  }

  // 사용자 반려동물 정보를 바탕으로 맞춤 추천 수행
  private List<RecommendationResponse> searchByUserPet(UserPet pet) {
    // 반려동물 성향 정보 추출
    PetType type = pet.getType();
    ActivityLevel activity = pet.getActivityLevel();
    Personality personality = pet.getPersonality();

    // 반려동물 성향에 따른 시설 점수 맵 생성
    Map<AccommodationPetFacilityType, Integer> accScoreMap = PetFacilityScoreMap.getAccommodationScore(
        type, activity, personality);
    Map<RoomPetFacilityType, Integer> roomScoreMap = PetFacilityScoreMap.getRoomScore(type,
        activity, personality);

    // allowedPetTypes 필드 기반 필터링 쿼리 생성
    Query query = buildPetTypeQuery(type.name());

    // 정렬 없이 (점수로 후처리 예정) 검색 요청 생성 (source 필터링 생략)
    SearchRequest request = buildSearchRequest(query, null, false);

    try {
      // Elasticsearch에 요청 후 AccommodationDocument로 매핑된 결과 반환
      SearchResponse<AccommodationDocument> response =
          elasticsearchClient.search(request, AccommodationDocument.class);

      // 각 문서에 대해 점수 계산 후 정렬하여 RecommendationResponse로 변환
      return response.hits().hits().stream()
          .map(Hit::source)
          .filter(Objects::nonNull)
          .map(doc -> new AbstractMap.SimpleEntry<>(calculateScore(doc, accScoreMap, roomScoreMap),
              doc)) // 점수와 문서를 함께 보관
          .sorted((a, b) -> Integer.compare(b.getKey(), a.getKey())) // 점수 기준 내림차순 정렬
          .map(entry -> mapToResponse(entry.getValue())) // 최종 응답 객체로 매핑
          .toList();

    } catch (IOException e) {
      throw new MeongnyangerangException(ErrorCode.USER_RECOMMENDATION_FAILED);
    }
  }

  // allowedPetTypes에 정확히 일치하는 petType을 검색하는 쿼리 생성
  private Query buildPetTypeQuery(String petType) {
    return TermQuery.of(t -> t
        .field("allowedPetTypes")
        .value(petType)
    )._toQuery();
  }

  // 공통 SearchRequest 생성 메서드
  private SearchRequest buildSearchRequest(Query query, String sortField,
      boolean withSourceFilter) {
    return SearchRequest.of(s -> {
      // 기본 검색 조건 설정: 인덱스, 쿼리, 결과 크기
      SearchRequest.Builder builder = s.index(INDEX_NAME)
          .query(query)
          .size(SIZE);

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

  // 숙소의 시설 점수를 계산 (반려동물 성향 기반)
  private int calculateScore(AccommodationDocument doc,
      Map<AccommodationPetFacilityType, Integer> accMap,
      Map<RoomPetFacilityType, Integer> roomMap) {
    // 숙소 시설 점수 계산
    int accScore = doc.getAccommodationPetFacilities().stream()
        .map(AccommodationPetFacilityType::valueOf)
        .mapToInt(facility -> accMap.getOrDefault(facility, 0))
        .sum();

    // 객실 시설 점수 계산
    int roomScore = doc.getRoomPetFacilities().stream()
        .map(RoomPetFacilityType::valueOf)
        .mapToInt(facility -> roomMap.getOrDefault(facility, 0))
        .sum();

    // 총점 반환
    return accScore + roomScore;
  }

  // AccommodationDocument를 응답 객체로 변환
  private RecommendationResponse mapToResponse(AccommodationDocument doc) {
    return RecommendationResponse.builder()
        .id(doc.getId())
        .name(doc.getName())
        .price(doc.getPrice())
        .totalRating(doc.getTotalRating())
        .thumbnailUrl(doc.getThumbnailUrl())
        .build();
  }
}
