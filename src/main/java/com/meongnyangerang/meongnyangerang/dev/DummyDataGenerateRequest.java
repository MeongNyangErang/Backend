package com.meongnyangerang.meongnyangerang.dev;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;

@Getter
public class DummyDataGenerateRequest {

  @Min(value = 1, message = "최소 1개 이상의 회원을 생성해야 합니다")
  @Max(value = 500, message = "최대 500명까지 회원을 생성할 수 있습니다")
  private int userCount;

  @Min(value = 1, message = "최소 1개 이상의 숙소를 생성해야 합니다")
  @Max(value = 1000, message = "최대 1000개까지 숙소를 생성할 수 있습니다")
  private int accommodationCount;

  @Min(value = 1, message = "최소 객실 수는 1 이상이어야 합니다")
  @Max(value = 20, message = "최소 20개를 넘을 수 없습니다.")
  private int roomCount;

  @Min(value = 1, message = "예약 수는 1 이상이어야 합니다")
  @Max(value = 2000, message = "최대 2000개까지 예약을 생성할 수 있습니다")
  private int reservationCount;

  @Min(value = 1, message = "리뷰 수는 1이상이어야 합니다")
  @Max(value = 2000, message = "최대 2000개까지 리뷰를 생성할 수 있습니다")
  private int reviewCount;
}