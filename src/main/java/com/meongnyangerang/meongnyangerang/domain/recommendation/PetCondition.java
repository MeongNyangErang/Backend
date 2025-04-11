package com.meongnyangerang.meongnyangerang.domain.recommendation;

import com.meongnyangerang.meongnyangerang.domain.accommodation.PetType;
import com.meongnyangerang.meongnyangerang.domain.user.ActivityLevel;
import com.meongnyangerang.meongnyangerang.domain.user.Personality;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PetCondition {

  private final PetType petType;
  private final ActivityLevel activityLevel;
  private final Personality personality;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PetCondition that)) {
      return false;
    }

    return petType == that.petType && activityLevel == that.activityLevel
        && personality == that.personality;
  }

  @Override
  public int hashCode() {
    return Objects.hash(petType, activityLevel, personality);
  }

}
