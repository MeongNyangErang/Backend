package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationDocument;
import com.meongnyangerang.meongnyangerang.domain.accommodation.PetType;
import com.meongnyangerang.meongnyangerang.domain.room.Room;
import com.meongnyangerang.meongnyangerang.domain.user.ActivityLevel;
import com.meongnyangerang.meongnyangerang.domain.user.Personality;
import com.meongnyangerang.meongnyangerang.domain.user.UserPet;
import com.meongnyangerang.meongnyangerang.dto.accommodation.PetRecommendationGroup;
import com.meongnyangerang.meongnyangerang.dto.accommodation.RecommendationResponse;
import com.meongnyangerang.meongnyangerang.repository.UserPetRepository;
import com.meongnyangerang.meongnyangerang.repository.WishlistRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import com.meongnyangerang.meongnyangerang.repository.room.RoomRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccommodationRecommendationServiceTest {

  @Mock
  private ElasticsearchClient elasticsearchClient;

  @Mock
  private UserPetRepository userPetRepository;

  @Mock
  private AccommodationRepository accommodationRepository;

  @Mock
  private RoomRepository roomRepository;

  @Mock
  private WishlistRepository wishlistRepository;

  @InjectMocks
  private AccommodationRecommendationService recommendationService;

  @Test
  @DisplayName("비로그인 사용자 기본 추천 - 성공")
  void getDefaultRecommendations_success() throws IOException {
    // given - 테스트용 문서 준비
    AccommodationDocument doc1 = AccommodationDocument.builder()
        .id(1L)
        .name("test1 숙소")
        .thumbnailUrl("http://example.com/image1.jpg")
        .price(70000L)
        .totalRating(4.5)
        .allowedPetTypes(Set.of("SMALL_DOG", "LARGE_DOG"))
        .build();

    AccommodationDocument doc2 = AccommodationDocument.builder()
        .id(2L)
        .name("test2 숙소")
        .thumbnailUrl("http://example.com/image2.jpg")
        .price(80000L)
        .totalRating(4.7)
        .allowedPetTypes(Set.of("LARGE_DOG", "MEDIUM_DOG"))
        .build();

    AccommodationDocument doc3 = AccommodationDocument.builder()
        .id(3L)
        .name("test3 숙소")
        .thumbnailUrl("http://example.com/image3.jpg")
        .price(75000L)
        .totalRating(3.5)
        .allowedPetTypes(Set.of("CAT"))
        .build();

    List<AccommodationDocument> allDocs = List.of(doc1, doc2, doc3);

    when(elasticsearchClient.search(any(SearchRequest.class),
        eq(RecommendationResponse.class)))
        .thenAnswer(invocation -> {
          // SearchRequest 를 꺼냄
          SearchRequest request = invocation.getArgument(0);
          // petType 추출
          String petType = request.query().term().value().stringValue();

          // allowedPetTypes 에 petType 포함된 문서만 추출 (totalRating 내림차순 정렬)
          List<RecommendationResponse> filtered = allDocs.stream()
              .filter(doc -> doc.getAllowedPetTypes().contains(petType))
              .sorted(Comparator.comparingDouble(AccommodationDocument::getTotalRating).reversed())
              .map(this::convertToRecommendationResponse)
              .toList();

          return mockSearchResponse(filtered);
        });

    // when
    Map<String, List<RecommendationResponse>> result = recommendationService.getDefaultRecommendations();

    // then
    // 결과에 각 petType이 key로 들어 있는지 확인
    assertTrue(result.containsKey("소형견"));
    assertTrue(result.containsKey("대형견"));
    assertTrue(result.containsKey("중형견"));
    assertTrue(result.containsKey("고양이"));

    // 각 key에 대한 추천 숙소 수가 예상대로인지 확인
    assertEquals(1, result.get("소형견").size());
    assertEquals(2, result.get("대형견").size());
    assertEquals(1, result.get("중형견").size());
    assertEquals(1, result.get("고양이").size());

    // LARGE_DOG 추천 목록이 평점 높은 순으로 정렬되어 있는지 확인
    List<RecommendationResponse> largeDogList = result.get("대형견");
    assertEquals(4.7, largeDogList.get(0).getTotalRating());
    assertEquals(4.5, largeDogList.get(1).getTotalRating());
  }

  @Test
  @DisplayName("사용자가 등록한 반려동물 정보를 바탕으로 숙소를 추천해줘야 한다.")
  void getUserPetRecommendations_success() throws IOException {
    // given
    UserPet pet1 = UserPet.builder()
        .name("초코")
        .type(PetType.SMALL_DOG)
        .activityLevel(ActivityLevel.MEDIUM)
        .personality(Personality.EXTROVERT)
        .build();

    UserPet pet2 = UserPet.builder()
        .name("나비")
        .type(PetType.CAT)
        .activityLevel(ActivityLevel.LOW)
        .personality(Personality.INTROVERT)
        .build();

    List<UserPet> userPets = List.of(pet1, pet2);

    AccommodationDocument doc1 = AccommodationDocument.builder()
        .id(1L)
        .name("test1 숙소")
        .thumbnailUrl("http://example.com/image1.jpg")
        .price(70000L)
        .totalRating(4.5)
        .allowedPetTypes(Set.of("SMALL_DOG", "LARGE_DOG"))
        .accommodationPetFacilities(Set.of("PLAYGROUND", "PET_FOOD", "FENCE_AREA"))
        .roomPetFacilities(Set.of("EXCLUSIVE_YARD", "TOY", "PET_STEPS"))
        .build();

    AccommodationDocument doc2 = AccommodationDocument.builder()
        .id(2L)
        .name("test2 숙소")
        .thumbnailUrl("http://example.com/image2.jpg")
        .price(80000L)
        .totalRating(4.7)
        .allowedPetTypes(Set.of("LARGE_DOG", "MEDIUM_DOG"))
        .accommodationPetFacilities(Set.of("PLAYGROUND", "EXERCISE_AREA", "FENCE_AREA"))
        .roomPetFacilities(Set.of("EXCLUSIVE_YARD", "TOY", "POTTY_SUPPLIES"))
        .build();

    AccommodationDocument doc3 = AccommodationDocument.builder()
        .id(3L)
        .name("test3 숙소")
        .thumbnailUrl("http://example.com/image3.jpg")
        .price(75000L)
        .totalRating(3.5)
        .allowedPetTypes(Set.of("CAT"))
        .accommodationPetFacilities(Set.of("PET_FOOD", "NEARBY_HOSPITAL"))
        .roomPetFacilities(Set.of("CAT_TOWER", "TOY", "CAT_WHEEL"))
        .build();

    AccommodationDocument doc4 = AccommodationDocument.builder()
        .id(4L)
        .name("test4 숙소")
        .thumbnailUrl("http://example.com/image4.jpg")
        .price(80000L)
        .totalRating(4.7)
        .allowedPetTypes(Set.of("SMALL_DOG", "MEDIUM_DOG"))
        .accommodationPetFacilities(Set.of("PLAYGROUND", "PET_FOOD", "EXERCISE_AREA"))
        .roomPetFacilities(Set.of("EXCLUSIVE_YARD", "FENCE_AREA", "PET_STEPS"))
        .build();

    List<AccommodationDocument> allDocs = List.of(doc1, doc2, doc3, doc4);

    when(userPetRepository.findAllByUserId(1L)).thenReturn(userPets);

    when(elasticsearchClient.search(any(SearchRequest.class),
        eq(AccommodationDocument.class)))
        .thenAnswer(invocation -> {

          SearchRequest request = invocation.getArgument(0);
          String petType = request.query().term().value().stringValue();

          List<AccommodationDocument> filtered = allDocs.stream()
              .filter(doc -> doc.getAllowedPetTypes().contains(petType))
              .toList();

          return mockSearchResponses(filtered);
        });

    // when
    List<PetRecommendationGroup> result = recommendationService.getUserPetRecommendations(
        1L);

    // then
    assertEquals("초코", result.get(0).getPetName());
    assertEquals("나비", result.get(1).getPetName());

    List<RecommendationResponse> chocoList = result.get(0).getRecommendations();
    List<RecommendationResponse> nabiList = result.get(1).getRecommendations();

    assertEquals(2, chocoList.size());
    assertEquals(1, nabiList.size());

    assertEquals("test1 숙소", chocoList.get(0).getName());
    assertEquals("test4 숙소", chocoList.get(1).getName());
    assertEquals("test3 숙소", nabiList.get(0).getName());
  }

  @Test
  @DisplayName("조회수가 가장 높은 숙소 10개를 추천 - 성공")
  void getMostViewedRecommendations_success() {
    // given
    Long userId = 1L;
    List<Long> wishlisted = List.of(1L, 3L, 5L); // 찜한 숙소 ID

    List<Accommodation> accommodations = new ArrayList<>();
    for (int i = 1; i <= 11; i++) {
      accommodations.add(Accommodation.builder()
          .id((long) i)
          .name("숙소" + i)
          .thumbnailUrl("thumb" + i + ".jpg")
          .totalRating(4.0 + (i % 5))
          .viewCount((long) (100 + i)) // 101 ~ 111
          .build());
    }

    List<Accommodation> top10 = accommodations.stream()
        .sorted((a1, a2) -> Long.compare(a2.getViewCount(), a1.getViewCount()))
        .limit(10)
        .toList();

    when(accommodationRepository.findTop10ByOrderByViewCountDescTotalRatingDesc())
        .thenReturn(top10);

    for (Accommodation a : top10) {
      Room room = Room.builder()
          .id(a.getId() + 100)
          .price(40000L + a.getId() * 1000)
          .accommodation(a)
          .build();

      when(roomRepository.findFirstByAccommodationOrderByPriceAsc(a))
          .thenReturn(room);
    }

    when(wishlistRepository.findAccommodationIdsByUserId(userId)).thenReturn(wishlisted);

    // when
    List<RecommendationResponse> result = recommendationService.getMostViewedRecommendations(
        userId);

    // then
    assertEquals(10, result.size());
    assertEquals(top10.get(0).getName(), result.get(0).getName());

    for (int i = 0; i < result.size(); i++) {
      RecommendationResponse res = result.get(i);
      Accommodation expected = top10.get(i);

      assertEquals(expected.getId(), res.getId());
      assertEquals(expected.getName(), res.getName());
      assertEquals(expected.getThumbnailUrl(), res.getThumbnailUrl());
      assertEquals(expected.getTotalRating(), res.getTotalRating());
      assertEquals(wishlisted.contains(expected.getId()), res.isWishlisted());

      verify(accommodationRepository, times(1)).findTop10ByOrderByViewCountDescTotalRatingDesc();
      verify(wishlistRepository, times(1)).findAccommodationIdsByUserId(userId);
      for (Accommodation a : top10) {
        verify(roomRepository).findFirstByAccommodationOrderByPriceAsc(a);
      }
    }
  }

  private RecommendationResponse convertToRecommendationResponse(
      AccommodationDocument doc) {
    return RecommendationResponse.builder()
        .id(doc.getId())
        .name(doc.getName())
        .totalRating(doc.getTotalRating())
        .price(doc.getPrice())
        .build();
  }

  private SearchResponse<AccommodationDocument> mockSearchResponses(
      List<AccommodationDocument> docs) {
    // SearchResponse, HitsMetadata를 mock 객체로 생성
    SearchResponse<AccommodationDocument> response = mock(SearchResponse.class);
    HitsMetadata<AccommodationDocument> hits = mock(HitsMetadata.class);

    // 각 DefaultRecommendationResponse를 Elasticsearch의 Hit 객체로 매핑
    List<Hit<AccommodationDocument>> hitList = docs.stream()
        .map(doc -> Hit.<AccommodationDocument>of(h -> h
            .id(String.valueOf(doc.getId()))
            .index("accommodations")
            .source(doc)))
        .toList();

    // SearchResponse 내부의 hits 설정
    when(hits.hits()).thenReturn(hitList);
    when(response.hits()).thenReturn(hits);

    return response;
  }

  private SearchResponse<RecommendationResponse> mockSearchResponse(
      List<RecommendationResponse> docs) {
    // SearchResponse, HitsMetadata를 mock 객체로 생성
    SearchResponse<RecommendationResponse> response = mock(SearchResponse.class);
    HitsMetadata<RecommendationResponse> hits = mock(HitsMetadata.class);

    // 각 DefaultRecommendationResponse를 Elasticsearch의 Hit 객체로 매핑
    List<Hit<RecommendationResponse>> hitList = docs.stream()
        .map(doc -> Hit.<RecommendationResponse>of(h -> h
            .id(String.valueOf(doc.getId()))
            .index("accommodations")
            .source(doc)))
        .toList();

    // SearchResponse 내부의 hits 설정
    when(hits.hits()).thenReturn(hitList);
    when(response.hits()).thenReturn(hits);

    return response;
  }
}