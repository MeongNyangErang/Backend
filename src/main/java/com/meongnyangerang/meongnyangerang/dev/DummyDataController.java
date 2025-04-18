package com.meongnyangerang.meongnyangerang.dev;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dummy-data")
@RequiredArgsConstructor
public class DummyDataController {

  private final DummyDataCreateService dataCreateService;
  private final DummyDataDeleteService dummyDataDeleteService;

  /**
   * 더미 데이터 생성 API
   */
  @PostMapping
  public ResponseEntity<Map<String, Object>> generateDummyDate(
      @Valid @RequestBody DummyDataGenerateRequest request
  ) {
    return ResponseEntity.ok(dataCreateService.generateData(request));
  }

  /**
   * 더미 데이터 삭제 API
   */
  @DeleteMapping
  public ResponseEntity<Map<String, Object>> clearDummyData() {
    // 더미 데이터 삭제
    Map<String, Object> result = dummyDataDeleteService.clearData();

    return ResponseEntity.ok(result);
  }

  /**
   * 더미 데이터 상태 확인 API
   */
  @GetMapping
  public ResponseEntity<Map<String, Object>> getDummyDataStatus() {
    Map<String, Object> status = Map.of(
        "hosts", dataCreateService.getHostCount(),
        "accommodations", dataCreateService.getAccommodationCount(),
        "rooms", dataCreateService.getRoomCount(),
        "users", dataCreateService.getUserCount(),
        "reservations", dataCreateService.getReservationCount(),
        "reviews", dataCreateService.getReviewCount()
    );
    return ResponseEntity.ok(status);
  }
}
