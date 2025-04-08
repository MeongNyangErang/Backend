package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.*;

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
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.ReservationSlotRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccommodationSearchService {

  private final ElasticsearchClient elasticsearchClient;
  private final ReservationSlotRepository reservationSlotRepository;

  public PageResponse<AccommodationSearchResponse> searchAccommodation(AccommodationSearchRequest request,
      Pageable pageable) {

    List<Query> mustQueries = new ArrayList<>();
    List<Query> mustNotQueries = new ArrayList<>();

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

    // 예약된 객실 제외 (must_not)
    List<Long> reservedRoomIds = reservationSlotRepository.findReservedRoomIdsBetweenDates(
        request.getCheckInDate(), request.getCheckOutDate().minusDays(1));  // 체크아웃날은 제외

    if (!reservedRoomIds.isEmpty()) {
      mustNotQueries.add(Query.of(q -> q
          .terms(t -> t
              .field("roomId")
              .terms(terms -> terms
                  .value(reservedRoomIds.stream().map(FieldValue::of).toList())
              )
          )));
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
        .bool(b -> b
            .must(mustQueries)
            .mustNot(mustNotQueries)));

    try {
      SearchResponse<AccommodationRoomDocument> response = elasticsearchClient.search(s -> s
              .index("accommodation_room")
              .query(finalQuery)
              .size(pageable.getPageSize())
              .from((int) pageable.getOffset())
              .sort(so -> so
                  .field(f -> f
                      .field("totalRating")
                      .order(SortOrder.Desc))),
          AccommodationRoomDocument.class
      );

      // 중복 제거
      Map<Long, AccommodationRoomDocument> unique = new LinkedHashMap<>();
      for (Hit<AccommodationRoomDocument> hit : response.hits().hits()) {
        AccommodationRoomDocument doc = hit.source();
        if (doc != null) {
          unique.putIfAbsent(doc.getAccommodationId(), doc); // 숙소 ID 기준 중복 제거
        }
      }

      List<AccommodationSearchResponse> content = unique.values().stream()
          .map(AccommodationSearchResponse::fromDocument)
          .toList();

      // total count 가져오는 방식은 정확도에 따라 다름 (지금은 hits.total.value 사용)
      long totalElements = response.hits().total().value(); // 총 개수
      int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());

      return new PageResponse<>(
          content,
          pageable.getPageNumber(),
          pageable.getPageSize(),
          totalElements,
          totalPages,
          pageable.getPageNumber() == 0,
          (pageable.getOffset() + pageable.getPageSize()) >= totalElements // 마지막 페이지 여부
      );

    } catch (IOException e) {
      throw new MeongnyangerangException(SEARCH_FAILED);
    }
  }

  private void applyTermsFilter(List<Query> mustQueries, String field, List<String> values) {
    if (values != null && !values.isEmpty()) {
      mustQueries.add(Query.of(q -> q
          .terms(t -> t
              .field(field + ".keyword")
              .terms(terms -> terms
                  .value(values.stream().map(FieldValue::of).toList())
              )
          )
      ));
    }
  }
}

