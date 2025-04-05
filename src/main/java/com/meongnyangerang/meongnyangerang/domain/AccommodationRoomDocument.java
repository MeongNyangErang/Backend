package com.meongnyangerang.meongnyangerang.domain;

import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationType;
import jakarta.persistence.Id;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "accommodation_room") // Elasticsearch 의 인덱스 이름
public class AccommodationRoomDocument {

  @Id
  private String id; // ex: "1_3" → 숙소ID_객실ID

  private Long accommodationId;
  private Long roomId;

  private String accommodationName;
  private String roomName;

  private String address;

  private AccommodationType accommodationType;
  private Double totalRating;

  private Long price;

  private Integer standardPeopleCount;
  private Integer maxPeopleCount;
  private Integer standardPetCount;
  private Integer maxPetCount;

  private List<String> accommodationFacilities;
  private List<String> accommodationPetFacilities;
  private List<String> roomFacilities;
  private List<String> roomPetFacilities;
  private List<String> hashtags;
  private List<String> allowPets;
}
