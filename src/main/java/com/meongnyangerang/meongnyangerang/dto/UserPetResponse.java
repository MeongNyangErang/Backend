package com.meongnyangerang.meongnyangerang.dto;

import com.meongnyangerang.meongnyangerang.domain.user.UserPet;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserPetResponse {

  private Long petId;
  private String name;
  private String birthDate; // yyyy-MM-dd 형식
  private String type;
  private String personality;
  private String activityLevel;

  public static UserPetResponse from(UserPet pet) {
    return new UserPetResponse(
        pet.getId(),
        pet.getName(),
        pet.getBirthDate().toString(),
        pet.getType().name(),
        pet.getPersonality().name(),
        pet.getActivityLevel().name()
    );
  }
}
