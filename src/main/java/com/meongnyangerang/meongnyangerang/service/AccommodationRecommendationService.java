package com.meongnyangerang.meongnyangerang.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.meongnyangerang.meongnyangerang.domain.accommodation.PetType;
import com.meongnyangerang.meongnyangerang.dto.accommodation.DefaultRecommendationResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccommodationRecommendationService {

  private final ElasticsearchClient elasticsearchClient;

  private static final String INDEX_NAME = "accommodations";
  private static final int SIZE = 6;

  // 비로그인 사용자 기본 추천
  public Map<String, List<DefaultRecommendationResponse>> getDefaultRecommendations() {
    Map<String, List<DefaultRecommendationResponse>> result = new HashMap<>();

    // 각 petType 별로 인기 숙소 검색하여 결과에 추가
    for (PetType petType : PetType.values()) {
      List<DefaultRecommendationResponse> docs = searchByPetType(petType.name());
      result.put(petType.name(), docs);
    }

    return result;
  }


  // 반려동물 타입에 대한 숙소 검색. totalRating 내림차순으로 정렬하여 최대 SIZE 만큼 가져옴
  private List<DefaultRecommendationResponse> searchByPetType(String petType) {
    try {
      // 쿼리 설정 (검색 조건) - allowedPetTypes 필드에 petType 과 정확히 일치하는 문서 검색
      Query petTypeQuery = TermQuery.of(t -> t
          .field("allowedPetTypes")
          .value(petType)
      )._toQuery();

      // 검색 요청 생성 (전체 쿼리) - 인덱스에서 조건에 맞는 문서를 totalRating 내림차순으로 최대 SIZE개 조회
      SearchRequest request = SearchRequest.of(s -> s.index(INDEX_NAME)
          .query(petTypeQuery)
          .sort(sort -> sort
              .field(f -> f
                  .field("totalRating")
                  .order(SortOrder.Desc)
              )
          )
          .size(SIZE)
      );

      // Elasticsearch 에 요청 전송 후, 응답의 _source 부분을 DefaultRecommendationResponse 로 자동 매핑하여 받음
      SearchResponse<DefaultRecommendationResponse> response =
          elasticsearchClient.search(request, DefaultRecommendationResponse.class);

      // 검색 응답(Hits)에서 DefaultRecommendationResponse 만 추출하여 리스트로 정리
      return response.hits().hits()
          .stream()
          .map(Hit::source)
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new MeongnyangerangException(ErrorCode.DEFAULT_RECOMMENDATION_FAILED);
    }
  }
}
