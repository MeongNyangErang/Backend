package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationDocument;
import com.meongnyangerang.meongnyangerang.dto.accommodation.DefaultRecommendationResponse;
import java.io.IOException;
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
        eq(DefaultRecommendationResponse.class)))
        .thenAnswer(invocation -> {
          // SearchRequest 를 꺼냄
          SearchRequest request = invocation.getArgument(0);
          // petType 추출
          String petType = request.query().term().value().stringValue();

          // allowedPetTypes 에 petType 포함된 문서만 추출 (totalRating 내림차순 정렬)
          List<DefaultRecommendationResponse> filtered = allDocs.stream()
              .filter(doc -> doc.getAllowedPetTypes().contains(petType))
              .sorted(Comparator.comparingDouble(AccommodationDocument::getTotalRating).reversed())
              .map(this::convertToDefaultRecommendationResponse)
              .toList();

          return mockSearchResponse(filtered);
        });

    // when
    Map<String, List<DefaultRecommendationResponse>> result = recommendationService.getDefaultRecommendations();

    // then
    // 결과에 각 petType이 key로 들어 있는지 확인
    assertTrue(result.containsKey("SMALL_DOG"));
    assertTrue(result.containsKey("LARGE_DOG"));
    assertTrue(result.containsKey("MEDIUM_DOG"));
    assertTrue(result.containsKey("CAT"));

    // 각 key에 대한 추천 숙소 수가 예상대로인지 확인
    assertEquals(1, result.get("SMALL_DOG").size());
    assertEquals(2, result.get("LARGE_DOG").size());
    assertEquals(1, result.get("MEDIUM_DOG").size());
    assertEquals(1, result.get("CAT").size());

    // LARGE_DOG 추천 목록이 평점 높은 순으로 정렬되어 있는지 확인
    List<DefaultRecommendationResponse> largeDogList = result.get("LARGE_DOG");
    assertEquals(4.7, largeDogList.get(0).getTotalRating());
    assertEquals(4.5, largeDogList.get(1).getTotalRating());
  }

  private DefaultRecommendationResponse convertToDefaultRecommendationResponse(
      AccommodationDocument doc) {
    return DefaultRecommendationResponse.builder()
        .id(doc.getId())
        .name(doc.getName())
        .totalRating(doc.getTotalRating())
        .price(doc.getPrice())
        .build();
  }

  private SearchResponse<DefaultRecommendationResponse> mockSearchResponse(
      List<DefaultRecommendationResponse> docs) {
    // SearchResponse, HitsMetadata를 mock 객체로 생성
    SearchResponse<DefaultRecommendationResponse> response = mock(SearchResponse.class);
    HitsMetadata<DefaultRecommendationResponse> hits = mock(HitsMetadata.class);

    // 각 DefaultRecommendationResponse를 Elasticsearch의 Hit 객체로 매핑
    List<Hit<DefaultRecommendationResponse>> hitList = docs.stream()
        .map(doc -> Hit.<DefaultRecommendationResponse>of(h -> h
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