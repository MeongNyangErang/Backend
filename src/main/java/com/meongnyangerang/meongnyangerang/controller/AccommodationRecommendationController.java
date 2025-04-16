package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.domain.accommodation.PetType;
import com.meongnyangerang.meongnyangerang.dto.accommodation.PetRecommendationGroup;
import com.meongnyangerang.meongnyangerang.dto.accommodation.RecommendationResponse;
import com.meongnyangerang.meongnyangerang.dto.chat.PageResponse;
import com.meongnyangerang.meongnyangerang.security.UserDetailsImpl;
import com.meongnyangerang.meongnyangerang.service.AccommodationRecommendationService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recommendations")
public class AccommodationRecommendationController {

  private final AccommodationRecommendationService recommendationService;

  // 비로그인 사용자 기본 추천
  @GetMapping("/default")
  public ResponseEntity<Map<String, List<RecommendationResponse>>> getDefaultRecommendations() {

    return ResponseEntity.ok(recommendationService.getDefaultRecommendations());
  }

  // 비로그인 사용자 기본 추천 더보기
  @GetMapping("/default/more")
  public ResponseEntity<PageResponse<RecommendationResponse>> getDefaultLoadMoreRecommendations(
      @RequestParam PetType type,
      @PageableDefault(size = 20) Pageable pageable) {

    return ResponseEntity.ok(
        recommendationService.getDefaultLoadMoreRecommendations(type, pageable));
  }

  // 사용자가 등록한 반려동물 기반 추천
  @GetMapping("/user-pet")
  public ResponseEntity<List<PetRecommendationGroup>> getUserPetRecommendations(
      @AuthenticationPrincipal UserDetailsImpl userDetails) {

    return ResponseEntity.ok(recommendationService.getUserPetRecommendations(userDetails.getId()));
  }

  // 사용자가 등록한 반려동물 기반 추천 더보기
  @GetMapping("/user-pet/more")
  public ResponseEntity<PageResponse<RecommendationResponse>> getUserPetLoadMoreRecommendations(
      @AuthenticationPrincipal UserDetailsImpl userDetails,
      @RequestParam Long petId,
      @PageableDefault(size = 20) Pageable pageable) {

    return ResponseEntity.ok(
        recommendationService.getUserPetLoadMoreRecommendations(userDetails.getId(), petId,
            pageable));
  }

  // 많은 사람들이 관심을 가진 숙소 추천
  @GetMapping("/most-viewed")
  public ResponseEntity<List<RecommendationResponse>> getPopularRecommendations() {

    return ResponseEntity.ok(recommendationService.getMostViewedRecommendations());
  }

}
