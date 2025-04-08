package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.domain.accommodation.AccommodationDocument;
import com.meongnyangerang.meongnyangerang.service.AccommodationRecommendationService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recommendations")
public class AccommodationRecommendationController {

  private final AccommodationRecommendationService recommendationService;

  // 비로그인 사용자 기본 추천
  @GetMapping("/default")
  public ResponseEntity<Map<String, List<AccommodationDocument>>> getDefaultRecommendations() {

    return ResponseEntity.ok(recommendationService.getDefaultRecommendations());
  }
}
