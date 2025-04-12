package com.meongnyangerang.meongnyangerang.dto.accommodation;

import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationSearchRequest {

  @NotBlank(message = "위치는 필수입니다.")
  private String location;

  @NotNull(message = "체크인 날짜는 필수입니다.")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate checkInDate;

  @NotNull(message = "체크아웃 날짜는 필수입니다.")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate checkOutDate;

  @NotNull(message = "인원 수는 필수입니다.")
  @Min(value = 1, message = "최소 1명 이상이어야 합니다.")
  private Integer peopleCount;

  @NotNull(message = "반려동물 수는 필수입니다.")
  @Min(value = 0, message = "0 이상이어야 합니다.")
  private Integer petCount;

  // 선택 필터들
  private AccommodationType accommodationType;
  private Long minPrice;
  private Long maxPrice;
  private Double minRating;

  private List<String> accommodationFacilities;
  private List<String> accommodationPetFacilities;
  private List<String> roomFacilities;
  private List<String> roomPetFacilities;
  private List<String> hashtags;
  private List<String> allowPets;
}
