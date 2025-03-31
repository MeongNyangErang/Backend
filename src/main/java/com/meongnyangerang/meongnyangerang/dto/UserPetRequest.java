package com.meongnyangerang.meongnyangerang.dto;

import com.meongnyangerang.meongnyangerang.domain.accommodation.PetType;
import com.meongnyangerang.meongnyangerang.domain.user.ActivityLevel;
import com.meongnyangerang.meongnyangerang.domain.user.Personality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserPetRequest {

  @NotBlank(message = "반려동물 이름은 필수입니다.")
  private String name;

  @NotNull(message = "반려동물 생일은 필수입니다.")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate birthDate;

  @NotNull(message = "반려동물 타입은 필수입니다.")
  private PetType type;

  @NotNull(message = "성격은 필수입니다.")
  private Personality personality;

  @NotNull(message = "활동량은 필수입니다.")
  private ActivityLevel activityLevel;
}
