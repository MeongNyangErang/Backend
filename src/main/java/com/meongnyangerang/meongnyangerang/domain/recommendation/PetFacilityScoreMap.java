package com.meongnyangerang.meongnyangerang.domain.recommendation;

import com.meongnyangerang.meongnyangerang.domain.accommodation.PetType;
import com.meongnyangerang.meongnyangerang.domain.accommodation.facility.AccommodationPetFacilityType;
import com.meongnyangerang.meongnyangerang.domain.room.facility.RoomPetFacilityType;
import com.meongnyangerang.meongnyangerang.domain.user.ActivityLevel;
import com.meongnyangerang.meongnyangerang.domain.user.Personality;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class PetFacilityScoreMap {

  // 숙소 편의시설 점수 맵 (key: PetCondition, value: Map<AccommodationPetFacilityType, Integer>)
  private static final Map<PetCondition, Map<AccommodationPetFacilityType, Integer>> accommodationScoreMap = new HashMap<>();

  // 객실 편의시설 점수 맵 (key: PetCondition, value: Map<RoomPetFacilityType, Integer>)
  private static final Map<PetCondition, Map<RoomPetFacilityType, Integer>> roomScoreMap = new HashMap<>();

  static {
    // 각 조건 조합별로 점수를 등록
    // 소형견
    addScore(PetType.SMALL_DOG, ActivityLevel.LOW, Personality.INTROVERT,
        new int[]{1, 1, 0, 1, 3, 1, 1, 0, 2}, // 숙소 점수
        new int[]{1, 3, 0, 3, 0, 1, 2, 0, 0, 1, 2}); // 객실 점수

    addScore(PetType.SMALL_DOG, ActivityLevel.LOW, Personality.EXTROVERT,
        new int[]{2, 2, 1, 1, 1, 1, 1, 0, 2},
        new int[]{1, 2, 2, 2, 0, 1, 1, 0, 0, 1, 2});

    addScore(PetType.SMALL_DOG, ActivityLevel.MEDIUM, Personality.INTROVERT,
        new int[]{2, 1, 0, 2, 2, 1, 1, 0, 2},
        new int[]{1, 2, 1, 2, 0, 1, 2, 0, 0, 1, 2});

    addScore(PetType.SMALL_DOG, ActivityLevel.MEDIUM, Personality.EXTROVERT,
        new int[]{2, 2, 1, 2, 2, 2, 1, 1, 2},
        new int[]{1, 1, 2, 1, 0, 1, 1, 0, 0, 1, 2});

    addScore(PetType.SMALL_DOG, ActivityLevel.HIGH, Personality.INTROVERT,
        new int[]{2, 2, 0, 2, 2, 1, 1, 0, 2},
        new int[]{1, 2, 2, 3, 0, 2, 2, 0, 0, 1, 2});

    addScore(PetType.SMALL_DOG, ActivityLevel.HIGH, Personality.EXTROVERT,
        new int[]{3, 3, 1, 3, 1, 2, 1, 0, 2},
        new int[]{1, 2, 2, 2, 0, 2, 1, 0, 0, 1, 2});

    // 중형견
    addScore(PetType.MEDIUM_DOG, ActivityLevel.LOW, Personality.INTROVERT,
        new int[]{2, 1, 0, 1, 3, 1, 1, 0, 2},
        new int[]{1, 3, 2, 2, 0, 1, 2, 0, 0, 1, 2});

    addScore(PetType.MEDIUM_DOG, ActivityLevel.LOW, Personality.EXTROVERT,
        new int[]{2, 2, 0, 2, 1, 1, 1, 0, 2},
        new int[]{1, 2, 2, 2, 0, 1, 1, 0, 0, 1, 2});

    addScore(PetType.MEDIUM_DOG, ActivityLevel.MEDIUM, Personality.INTROVERT,
        new int[]{3, 2, 0, 2, 2, 1, 1, 0, 2},
        new int[]{1, 2, 2, 2, 0, 1, 2, 0, 0, 1, 2});

    addScore(PetType.MEDIUM_DOG, ActivityLevel.MEDIUM, Personality.EXTROVERT,
        new int[]{3, 2, 1, 2, 1, 2, 1, 1, 2},
        new int[]{1, 1, 2, 1, 0, 1, 1, 0, 0, 1, 2});

    addScore(PetType.MEDIUM_DOG, ActivityLevel.HIGH, Personality.INTROVERT,
        new int[]{3, 2, 1, 3, 2, 1, 1, 1, 2},
        new int[]{1, 2, 2, 2, 0, 2, 2, 0, 0, 1, 2});

    addScore(PetType.MEDIUM_DOG, ActivityLevel.HIGH, Personality.EXTROVERT,
        new int[]{3, 3, 2, 3, 1, 2, 1, 2, 2},
        new int[]{1, 1, 2, 2, 0, 2, 1, 0, 0, 1, 2});

    // 대형견
    addScore(PetType.LARGE_DOG, ActivityLevel.LOW, Personality.INTROVERT,
        new int[]{2, 1, 0, 1, 3, 1, 1, 0, 1},
        new int[]{1, 3, 2, 1, 0, 1, 2, 0, 0, 1, 2});

    addScore(PetType.LARGE_DOG, ActivityLevel.LOW, Personality.EXTROVERT,
        new int[]{2, 2, 0, 1, 2, 1, 1, 0, 1},
        new int[]{1, 2, 2, 1, 0, 1, 1, 0, 0, 1, 2});

    addScore(PetType.LARGE_DOG, ActivityLevel.MEDIUM, Personality.INTROVERT,
        new int[]{3, 1, 0, 2, 2, 1, 1, 0, 1},
        new int[]{1, 2, 2, 1, 0, 1, 2, 0, 0, 1, 2});

    addScore(PetType.LARGE_DOG, ActivityLevel.MEDIUM, Personality.EXTROVERT,
        new int[]{3, 2, 0, 2, 2, 2, 1, 0, 1},
        new int[]{1, 1, 2, 1, 0, 1, 1, 0, 0, 1, 2});

    addScore(PetType.LARGE_DOG, ActivityLevel.HIGH, Personality.INTROVERT,
        new int[]{3, 2, 0, 3, 2, 1, 1, 0, 1},
        new int[]{1, 2, 2, 1, 0, 2, 2, 0, 0, 1, 2});

    addScore(PetType.LARGE_DOG, ActivityLevel.HIGH, Personality.EXTROVERT,
        new int[]{3, 3, 0, 3, 2, 2, 1, 0, 1},
        new int[]{1, 1, 2, 1, 0, 2, 1, 0, 0, 1, 2});

    // 고양이 (고양이는 일부 항목에 낮은 점수 또는 0점 처리)
    addScore(PetType.CAT, ActivityLevel.LOW, Personality.INTROVERT,
        new int[]{1, 1, 0, 1, 2, 0, 1, 0, 3},
        new int[]{1, 3, 1, 2, 0, 1, 2, 3, 2, 1, 1});

    addScore(PetType.CAT, ActivityLevel.LOW, Personality.EXTROVERT,
        new int[]{1, 2, 0, 1, 2, 0, 1, 0, 2},
        new int[]{1, 2, 1, 2, 0, 1, 2, 2, 2, 1, 1});

    addScore(PetType.CAT, ActivityLevel.MEDIUM, Personality.INTROVERT,
        new int[]{1, 2, 0, 1, 2, 0, 1, 0, 3},
        new int[]{1, 2, 1, 2, 0, 1, 2, 3, 3, 1, 1});

    addScore(PetType.CAT, ActivityLevel.MEDIUM, Personality.EXTROVERT,
        new int[]{1, 2, 0, 1, 2, 0, 1, 0, 2},
        new int[]{1, 2, 1, 2, 0, 1, 2, 2, 2, 1, 1});

    addScore(PetType.CAT, ActivityLevel.HIGH, Personality.INTROVERT,
        new int[]{1, 2, 0, 1, 2, 0, 1, 0, 3},
        new int[]{1, 2, 1, 2, 0, 2, 2, 3, 3, 1, 1});

    addScore(PetType.CAT, ActivityLevel.HIGH, Personality.EXTROVERT,
        new int[]{1, 2, 0, 1, 2, 0, 1, 0, 2},
        new int[]{1, 2, 1, 2, 0, 2, 2, 2, 2, 1, 1});
  }

  // 점수 추가 메서드
  private static void addScore(PetType type, ActivityLevel activity, Personality personality,
      int[] accommodationScores, int[] roomScores) {
    Map<AccommodationPetFacilityType, Integer> accMap = new EnumMap<>(AccommodationPetFacilityType.class);
    Map<RoomPetFacilityType, Integer> roomMap = new EnumMap<>(RoomPetFacilityType.class);

    AccommodationPetFacilityType[] accTypes = AccommodationPetFacilityType.values();
    RoomPetFacilityType[] roomTypes = RoomPetFacilityType.values();

    for (int i = 0; i < accTypes.length; i++) {
      accMap.put(accTypes[i], accommodationScores[i]);
    }
    for (int i = 0; i < roomTypes.length; i++) {
      roomMap.put(roomTypes[i], roomScores[i]);
    }

    PetCondition key = new PetCondition(type, activity, personality);
    accommodationScoreMap.put(key, accMap);
    roomScoreMap.put(key, roomMap);
  }

  // 점수 조회 메서드
  public static Map<AccommodationPetFacilityType, Integer> getAccommodationScore(PetType type, ActivityLevel activity, Personality personality) {
    return accommodationScoreMap.getOrDefault(new PetCondition(type, activity, personality), Collections.emptyMap());
  }

  public static Map<RoomPetFacilityType, Integer> getRoomScore(PetType type, ActivityLevel activity, Personality personality) {
    return roomScoreMap.getOrDefault(new PetCondition(type, activity, personality), Collections.emptyMap());
  }

}
