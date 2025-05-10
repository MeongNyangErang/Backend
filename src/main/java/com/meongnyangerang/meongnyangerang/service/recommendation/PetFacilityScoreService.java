package com.meongnyangerang.meongnyangerang.service.recommendation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

@Service
public class PetFacilityScoreService {

  private static final String JSON_FILE_PATH = "src/main/resources/pet_facility_scores.json";

  private final Map<String, ScoreDetail> scoreMap = new HashMap<>();

  @PostConstruct
  public void loadScoresFromJson() {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      List<PetFacilityScoreEntry> scoreEntries = objectMapper.readValue(
          new File(JSON_FILE_PATH),
          new TypeReference<>() {
          }
      );

      for (PetFacilityScoreEntry entry : scoreEntries) {
        String key = generateKey(entry.getPetType(), entry.getActivityLevel(),
            entry.getPersonality());
        scoreMap.put(key, new ScoreDetail(entry.getAccommodationScores(), entry.getRoomScores()));
      }
    } catch (IOException e) {
      throw new RuntimeException("Error loading pet facility scores from JSON", e);
    }
  }

  public Map<String, Integer> getAccommodationScore(String petType, String activityLevel,
      String personality) {
    ScoreDetail scoreDetail = scoreMap.get(generateKey(petType, activityLevel, personality));
    return scoreDetail != null ? scoreDetail.getAccommodationScores() : Collections.emptyMap();
  }

  public Map<String, Integer> getRoomScore(String petType, String activityLevel,
      String personality) {
    ScoreDetail scoreDetail = scoreMap.get(generateKey(petType, activityLevel, personality));
    return scoreDetail != null ? scoreDetail.getRoomScores() : Collections.emptyMap();
  }

  private String generateKey(String petType, String activityLevel, String personality) {
    return petType + "|" + activityLevel + "|" + personality;
  }

  @Getter
  @Setter
  public static class PetFacilityScoreEntry {

    private String petType;
    private String activityLevel;
    private String personality;
    private Map<String, Integer> accommodationScores;
    private Map<String, Integer> roomScores;
  }

  @Getter
  @Setter
  @AllArgsConstructor
  public static class ScoreDetail {

    private Map<String, Integer> accommodationScores;
    private Map<String, Integer> roomScores;
  }
}
