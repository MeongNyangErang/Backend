package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.accommodation.PetType;
import com.meongnyangerang.meongnyangerang.domain.user.ActivityLevel;
import com.meongnyangerang.meongnyangerang.domain.user.Personality;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.UserPet;
import com.meongnyangerang.meongnyangerang.dto.UserPetRequest;
import com.meongnyangerang.meongnyangerang.repository.UserPetRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserPetServiceTest {

  @InjectMocks
  private UserPetService userPetService;

  @Mock
  private UserPetRepository userPetRepository;

  @Mock
  private UserRepository userRepository;

  @Test
  @DisplayName("반려동물 등록 성공")
  void registerPetSuccess() {
    // given
    Long userId = 1L;
    User user = User.builder().id(userId).email("user@example.com").build();

    UserPetRequest request = new UserPetRequest(
        "콩이",
        LocalDate.of(2020, 1, 1),
        PetType.SMALL_DOG,
        Personality.EXTROVERT,
        ActivityLevel.HIGH
    );

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userPetRepository.countByUserId(userId)).thenReturn(2L);

    // when
    userPetService.registerPet(userId, request);

    // then
    ArgumentCaptor<UserPet> captor = ArgumentCaptor.forClass(UserPet.class);
    verify(userPetRepository).save(captor.capture());

    UserPet savedPet = captor.getValue();
    assertEquals("콩이", savedPet.getName());
    assertEquals(LocalDate.of(2020, 1, 1), savedPet.getBirthDate());
    assertEquals(PetType.SMALL_DOG, savedPet.getType());
    assertEquals(Personality.EXTROVERT, savedPet.getPersonality());
    assertEquals(ActivityLevel.HIGH, savedPet.getActivityLevel());
    assertEquals(user, savedPet.getUser());
  }
}
