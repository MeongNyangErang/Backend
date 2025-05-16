package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.*;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.SEARCH_FAILED;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.meongnyangerang.meongnyangerang.domain.AccommodationRoomDocument;
import com.meongnyangerang.meongnyangerang.domain.accommodation.Accommodation;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationType;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationSearchRequest;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationSearchResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.ReservationSlotRepository;
import com.meongnyangerang.meongnyangerang.repository.WishlistRepository;
import com.meongnyangerang.meongnyangerang.repository.accommodation.AccommodationRepository;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccommodationSearchService {

  private final ElasticsearchClient elasticsearchClient;
  private final ReservationSlotRepository reservationSlotRepository;
  private final WishlistRepository wishlistRepository;
  private final AccommodationRepository accommodationRepository;

  public PageResponse<AccommodationSearchResponse> searchAccommodation(Long userId,
      AccommodationSearchRequest request,
      Pageable pageable) {

    List<Query> mustQueries = new ArrayList<>();
    List<Query> mustNotQueries = new ArrayList<>();

    // 위치 필터 (wildcard - 주소 전체에서 특정 키워드 포함 검색)
    applyLocationFilter(mustQueries, request.getLocation());

    // 인원 수 필터
    applyPeopleCountFilter(mustQueries, request.getPeopleCount());

    // 반려동물 수 필터
    applyPetCountFilter(mustQueries, request.getPetCount());

    // 숙소 유형
    applyAccommodationTypeFilter(mustQueries, request.getAccommodationType());

    // 가격 필터
    applyPriceFilter(mustQueries, request.getMinPrice(), request.getMaxPrice());

    // 평점 필터
    applyRatingFilter(mustQueries, request.getMinRating());

    // 예약된 객실 제외 (must_not)
    applyReservedRoomFilter(mustNotQueries, request.getCheckInDate(), request.getCheckOutDate());

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
              .from(Math.toIntExact(pageable.getOffset()))
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

      // 사용자가 찜한 숙소의 id를 Set에 저장
      Set<Long> wishlistedIds =
          (userId != null) ? new HashSet<>(wishlistRepository.findAccommodationIdsByUserId(userId))
              : Collections.emptySet();

      List<AccommodationSearchResponse> content = unique.values().stream()
          .map(doc -> {

            Accommodation accommodation = accommodationRepository.findById(doc.getAccommodationId())
                .orElseThrow(() -> new MeongnyangerangException(ACCOMMODATION_NOT_FOUND));

            return AccommodationSearchResponse.fromDocument(
                doc,
                wishlistedIds.contains(doc.getAccommodationId()),
                accommodation.getLatitude(),
                accommodation.getLongitude()
            );
          })
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

  private void applyLocationFilter(List<Query> mustQueries, String location) {
    if (location != null) {
      mustQueries.add(Query.of(q -> q
          .wildcard(w -> w
              .field("address")
              .wildcard("*" + location + "*")
          )));
    }
  }

  private void applyPeopleCountFilter(List<Query> mustQueries, Integer peopleCount) {
    if (peopleCount != null) {
      mustQueries.add(Query.of(q -> q
          .range(r -> r.field("standardPeopleCount").lte(JsonData.of(peopleCount)))));
      mustQueries.add(Query.of(q -> q
          .range(r -> r.field("maxPeopleCount").gte(JsonData.of(peopleCount)))));
    }
  }

  private void applyPetCountFilter(List<Query> mustQueries, Integer petCount) {
    if (petCount != null) {
      mustQueries.add(Query.of(q -> q
          .range(r -> r.field("standardPetCount").lte(JsonData.of(petCount)))));
      mustQueries.add(Query.of(q -> q
          .range(r -> r.field("maxPetCount").gte(JsonData.of(petCount)))));
    }
  }

  private void applyAccommodationTypeFilter(List<Query> mustQueries, AccommodationType type) {
    if (type != null) {
      mustQueries.add(Query.of(q -> q
          .term(t -> t
              .field("accommodationType.keyword")
              .value(type.name()))));
    }
  }

  private void applyPriceFilter(List<Query> mustQueries, Long minPrice, Long maxPrice) {
    if (minPrice != null || maxPrice != null) {
      mustQueries.add(Query.of(q -> q
          .range(r -> {
            r.field("price");
            if (minPrice != null) {
              r.gte(JsonData.of(minPrice));
            }
            if (maxPrice != null) {
              r.lte(JsonData.of(maxPrice));
            }
            return r;
          })
      ));
    }
  }

  private void applyRatingFilter(List<Query> mustQueries, Double minRating) {
    if (minRating != null) {
      mustQueries.add(Query.of(q -> q
          .range(r -> r
              .field("totalRating")
              .gte(JsonData.of(minRating)))));
    }
  }

  private void applyReservedRoomFilter(List<Query> mustNotQueries, LocalDate checkInDate,
      LocalDate checkOutDate) {
    List<Long> reservedRoomIds = reservationSlotRepository.findReservedRoomIdsBetweenDates(
        checkInDate, checkOutDate.minusDays(1));

    if (!reservedRoomIds.isEmpty()) {
      mustNotQueries.add(Query.of(q -> q
          .terms(t -> t
              .field("roomId")
              .terms(terms -> terms
                  .value(reservedRoomIds.stream().map(FieldValue::of).toList())
              )
          )));
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