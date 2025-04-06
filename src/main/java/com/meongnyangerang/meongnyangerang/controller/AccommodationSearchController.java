package com.meongnyangerang.meongnyangerang.controller;

import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationSearchRequest;
import com.meongnyangerang.meongnyangerang.dto.accommodation.AccommodationSearchResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search/accommodations")
public class AccommodationSearchController {

  private final AccommodationSearchService searchService;

  @PostMapping
  public ResponseEntity<List<AccommodationSearchResponse>> searchAccommodation(
      @Valid @RequestBody AccommodationSearchRequest request) {

    return ResponseEntity.ok(searchService.searchAccommodation(request));
  }
}
