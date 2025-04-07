package com.meongnyangerang.meongnyangerang.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.meongnyangerang.meongnyangerang.domain.AccommodationRoomDocument;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationSearchRequest;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationSearchResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccommodationSearchService {

  private final ElasticsearchClient elasticsearchClient;

  public List<AccommodationSearchResponse> searchAccommodation(AccommodationSearchRequest request) {
    List<Query> mustQueries = new ArrayList<>();

    // 위치 필터 (wildcard - 주소 전체에서 특정 키워드 포함 검색)
    if (request.getLocation() != null) {
      mustQueries.add(Query.of(q -> q
          .wildcard(w -> w
              .field("address")
              .wildcard("*" + request.getLocation() + "*")
          )));
    }

    // 인원 수 필터
    if (request.getPeopleCount() != null) {
      mustQueries.add(Query.of(q -> q
          .range(r -> r.field("standardPeopleCount").lte(JsonData.of(request.getPeopleCount())))));
      mustQueries.add(Query.of(q -> q
          .range(r -> r.field("maxPeopleCount").gte(JsonData.of(request.getPeopleCount())))));
    }

    // 반려동물 수 필터
    if (request.getPetCount() != null) {
      mustQueries.add(Query.of(q -> q
          .range(r -> r.field("standardPetCount").lte(JsonData.of(request.getPetCount())))));
      mustQueries.add(Query.of(q -> q
          .range(r -> r.field("maxPetCount").gte(JsonData.of(request.getPetCount())))));
    }

    // 숙소 유형
    if (request.getAccommodationType() != null) {
      mustQueries.add(Query.of(q -> q
          .term(t -> t
              .field("accommodationType.keyword") // enum은 keyword 필드 기준
              .value(request.getAccommodationType().name()))));
    }

    // 가격 필터
    if (request.getMinPrice() != null || request.getMaxPrice() != null) {
      mustQueries.add(Query.of(q -> q
          .range(r -> {
            r.field("price");
            if (request.getMinPrice() != null) {
              r.gte(JsonData.of(request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
              r.lte(JsonData.of(request.getMaxPrice()));
            }
            return r;
          })
      ));
    }

    // 평점 필터
    if (request.getMinRating() != null) {
      mustQueries.add(Query.of(q -> q
          .range(r -> r
              .field("totalRating")
              .gte(JsonData.of(request.getMinRating())))));
    }

    // terms 필터들
    applyTermsFilter(mustQueries, "accommodationFacilities", request.getAccommodationFacilities());
    applyTermsFilter(mustQueries, "accommodationPetFacilities",
        request.getAccommodationPetFacilities());
    applyTermsFilter(mustQueries, "roomFacilities", request.getRoomFacilities());
    applyTermsFilter(mustQueries, "roomPetFacilities", request.getRoomPetFacilities());
    applyTermsFilter(mustQueries, "hashtags", request.getHashtags());
    applyTermsFilter(mustQueries, "allowPets", request.getAllowPets());

    // Bool Query 조합
    Query finalQuery = Query.of(q -> q
        .bool(b -> b.must(mustQueries)));

    try {
      SearchResponse<AccommodationRoomDocument> response = elasticsearchClient.search(s -> s
              .index("accommodation_room")
              .query(finalQuery)
              .size(100)
              .sort(so -> so
                  .field(f -> f
                      .field("totalRating")
                      .order(SortOrder.Desc))),
          AccommodationRoomDocument.class
      );

      Map<Long, AccommodationRoomDocument> unique = new LinkedHashMap<>();
      for (Hit<AccommodationRoomDocument> hit : response.hits().hits()) {
        AccommodationRoomDocument doc = hit.source();
        if (doc != null) {
          unique.putIfAbsent(doc.getAccommodationId(), doc);
        }
      }

      return unique.values().stream()
          .map(AccommodationSearchResponse::fromDocument)
          .toList();

    } catch (IOException e) {
      throw new RuntimeException("Elasticsearch 검색 실패", e);
    }
  }

  private void applyTermsFilter(List<Query> mustQueries, String field, List<String> values) {
    if (values != null && !values.isEmpty()) {
      mustQueries.add(Query.of(q -> q
          .terms(t -> t
              .field(field)
              .terms(terms -> terms
                  .value(values.stream().map(FieldValue::of).toList())
              )
          )
      ));
    }
  }
}

