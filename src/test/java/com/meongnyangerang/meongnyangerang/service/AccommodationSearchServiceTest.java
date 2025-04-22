package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.SEARCH_FAILED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import com.meongnyangerang.meongnyangerang.domain.AccommodationRoomDocument;
import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationType;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationSearchRequest;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationSearchResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.ReservationSlotRepository;
import com.meongnyangerang.meongnyangerang.repository.WishlistRepository;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AccommodationSearchServiceTest {

  @Mock
  private ElasticsearchClient elasticsearchClient;

  @Mock
  private ReservationSlotRepository reservationSlotRepository;

  @Mock
  private WishlistRepository wishlistRepository;

  @InjectMocks
  private AccommodationSearchService searchService;

  @Test
  @DisplayName("숙소 목록 조회(필터) - 성공")
  void searchAccommodation_Success_MultipleHits() throws Exception {
    // given
    Long userId = 1L;
    AccommodationSearchRequest request = new AccommodationSearchRequest(
        "서울",
        LocalDate.of(2025, 4, 18),
        LocalDate.of(2025, 4, 19),
        2, 1,
        AccommodationType.PENSION,
        50000L, 150000L, 4.0,
        List.of("WIFI", "BREAKFAST"),
        List.of("SHOWER_ROOM", "SWIMMING_POOL"),
        List.of("AIR_CONDITIONER", "TV"),
        List.of("FOOD_BOWL", "TOY"),
        List.of("FAMILY_TRIP", "OCEAN_VIEW"),
        List.of("SMALL_DOG", "CAT")
    );

    AccommodationRoomDocument doc1 = AccommodationRoomDocument.builder()
        .accommodationId(1L)
        .roomId(101L)
        .accommodationName("서울의 휴양지")
        .roomName("스위트룸")
        .address("서울 강남구")
        .thumbnailUrl("https://img.example.com/room1.jpg")
        .totalRating(4.7)
        .price(120000L)
        .standardPeopleCount(2)
        .maxPeopleCount(4)
        .standardPetCount(1)
        .maxPetCount(2)
        .accommodationType(AccommodationType.PENSION)
        .accommodationFacilities(List.of("WIFI", "BREAKFAST"))
        .accommodationPetFacilities(List.of("SHOWER_ROOM", "SWIMMING_POOL"))
        .roomFacilities(List.of("AIR_CONDITIONER", "TV"))
        .roomPetFacilities(List.of("FOOD_BOWL", "TOY"))
        .hashtags(List.of("FAMILY_TRIP", "OCEAN_VIEW"))
        .allowPets(List.of("SMALL_DOG", "CAT"))
        .build();

    AccommodationRoomDocument doc2 = AccommodationRoomDocument.builder()
        .accommodationId(2L)
        .roomId(201L)
        .accommodationName("서울의 감성숙소")
        .roomName("디럭스룸")
        .address("서울 마포구")
        .thumbnailUrl("https://img.example.com/room2.jpg")
        .totalRating(4.8)
        .price(95000L)
        .standardPeopleCount(2)
        .maxPeopleCount(3)
        .standardPetCount(1)
        .maxPetCount(2)
        .accommodationType(AccommodationType.PENSION)
        .accommodationFacilities(List.of("WIFI"))
        .accommodationPetFacilities(List.of("PLAYGROUND"))
        .roomFacilities(List.of("AIR_CONDITIONER"))
        .roomPetFacilities(List.of("FOOD_BOWL"))
        .hashtags(List.of("COZY"))
        .allowPets(List.of("SMALL_DOG"))
        .build();

    Hit<AccommodationRoomDocument> hit1 = Hit.of(h -> h
        .index("accommodation_room").id("1_101").source(doc1));
    Hit<AccommodationRoomDocument> hit2 = Hit.of(h -> h
        .index("accommodation_room").id("2_201").source(doc2));

    SearchResponse<AccommodationRoomDocument> mockResponse = SearchResponse.of(s -> s
        .took(15)
        .timedOut(false)
        .shards(sh -> sh.total(1).successful(1).skipped(0).failed(0))
        .hits(h -> h
            .hits(List.of(hit1, hit2))
            .total(t -> t.value(2L).relation(TotalHitsRelation.Eq))
        )
    );

    given(reservationSlotRepository.findReservedRoomIdsBetweenDates(
        request.getCheckInDate(), request.getCheckOutDate().minusDays(1)))
        .willReturn(List.of());

    given(wishlistRepository.findAccommodationIdsByUserId(userId))
        .willReturn(List.of(1L));

    given(elasticsearchClient.search(any(Function.class), eq(AccommodationRoomDocument.class)))
        .willReturn(mockResponse);

    // when
    PageResponse<AccommodationSearchResponse> response =
        searchService.searchAccommodation(userId, request, PageRequest.of(0, 20));

    // then
    assertThat(response.content()).hasSize(2);
    assertThat(response.content()).extracting("accommodationId")
        .containsExactlyInAnyOrder(1L, 2L);
    assertThat(response.content()).extracting("isWishlisted")
        .containsExactlyInAnyOrder(true, false);
  }

  @Test
  @DisplayName("숙소 목록 조회 실패 - Elasticsearch IOException 발생")
  void searchAccommodation_ElasticsearchError_ThrowsException() throws Exception {
    // given
    AccommodationSearchRequest request = new AccommodationSearchRequest(
        "서울",
        LocalDate.of(2025, 4, 18),
        LocalDate.of(2025, 4, 19),
        2, 1,
        AccommodationType.PENSION,
        50000L, 150000L, 4.0,
        List.of("WIFI", "BREAKFAST"),
        List.of("SHOWER_ROOM", "SWIMMING_POOL"),
        List.of("AIR_CONDITIONER", "TV"),
        List.of("FOOD_BOWL", "TOY"),
        List.of("FAMILY_TRIP", "OCEAN_VIEW"),
        List.of("SMALL_DOG", "CAT")
    );

    given(reservationSlotRepository.findReservedRoomIdsBetweenDates(
        request.getCheckInDate(), request.getCheckOutDate().minusDays(1)))
        .willReturn(List.of());

    given(elasticsearchClient.search(any(Function.class), eq(AccommodationRoomDocument.class)))
        .willThrow(new IOException("Elasticsearch 연결 실패"));

    // when & then
    assertThatThrownBy(() -> searchService.searchAccommodation(1L, request, PageRequest.of(0, 20)))
        .isInstanceOf(MeongnyangerangException.class)
        .extracting("errorCode")
        .isEqualTo(SEARCH_FAILED);
  }
}


