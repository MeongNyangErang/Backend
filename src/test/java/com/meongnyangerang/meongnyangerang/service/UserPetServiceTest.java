package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.accommodation.PetType;
import com.meongnyangerang.meongnyangerang.domain.user.ActivityLevel;
import com.meongnyangerang.meongnyangerang.domain.user.Personality;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.UserPet;
import com.meongnyangerang.meongnyangerang.dto.UserPetRequest;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
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

  @Test
  @DisplayName("반려동물 10마리 초과 등록 시 실패")
  void registerPetFailDueToLimit() {
    // given
    Long userId = 2L;
    User user = User.builder().id(userId).build();

    UserPetRequest request = new UserPetRequest(
        "냥이",
        LocalDate.of(2022, 6, 15),
        PetType.CAT,
        Personality.INTROVERT,
        ActivityLevel.LOW
    );

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userPetRepository.countByUserId(userId)).thenReturn(10L);

    // when & then
    assertThrows(MeongnyangerangException.class,
        () -> userPetService.registerPet(userId, request));
  }

  @Test
  @DisplayName("반려동물 수정 성공")
  void updatePetSuccess() {
    // given
    Long userId = 1L;
    Long petId = 100L;

    User user = User.builder().id(userId).build();
    UserPet pet = UserPet.builder().id(petId).user(user).build();

    UserPetRequest request = new UserPetRequest("콩이", LocalDate.of(2020, 1, 1),
        PetType.SMALL_DOG, Personality.EXTROVERT, ActivityLevel.MEDIUM);

    when(userPetRepository.findById(petId)).thenReturn(Optional.of(pet));

    // when
    userPetService.updatePet(userId, petId, request);

    // then
    assertEquals("콩이", pet.getName());
    assertEquals(PetType.SMALL_DOG, pet.getType());
  }

  @Test
  @DisplayName("반려동물 수정 실패 - 소유자 불일치")
  void updatePetFailNotOwner() {
    // given
    Long userId = 1L;
    Long petId = 100L;

    User anotherUser = User.builder().id(2L).build();
    UserPet pet = UserPet.builder().id(petId).user(anotherUser).build();

    UserPetRequest request = new UserPetRequest("콩이", LocalDate.of(2020, 1, 1),
        PetType.SMALL_DOG, Personality.EXTROVERT, ActivityLevel.MEDIUM);

    when(userPetRepository.findById(petId)).thenReturn(Optional.of(pet));

    // when & then
    assertThrows(MeongnyangerangException.class, () ->
        userPetService.updatePet(userId, petId, request));
  }

  @Test
  @DisplayName("반려동물 수정 실패 - 존재하지 않음")
  void updatePetFailNotFound() {
    // given
    Long userId = 1L;
    Long petId = 100L;

    UserPetRequest request = new UserPetRequest("콩이", LocalDate.of(2020, 1, 1),
        PetType.SMALL_DOG, Personality.EXTROVERT, ActivityLevel.MEDIUM);

    when(userPetRepository.findById(petId)).thenReturn(Optional.empty());

    // when & then
    assertThrows(MeongnyangerangException.class, () ->
        userPetService.updatePet(userId, petId, request));
  }

  @Test
  @DisplayName("반려동물 삭제 성공")
  void deletePetSuccess() {
    // given
    Long userId = 1L;
    Long petId = 100L;

    User user = User.builder().id(userId).build();
    UserPet pet = UserPet.builder().id(petId).user(user).build();

    when(userPetRepository.findById(petId)).thenReturn(Optional.of(pet));

    // when
    userPetService.deletePet(userId, petId);

    // then
    verify(userPetRepository).delete(pet);
  }

  @Test
  @DisplayName("반려동물 삭제 실패 - 소유자 불일치")
  void deletePetFailNotOwner() {
    // given
    Long userId = 1L;
    Long petId = 100L;

    User anotherUser = User.builder().id(2L).build();
    UserPet pet = UserPet.builder().id(petId).user(anotherUser).build();

    when(userPetRepository.findById(petId)).thenReturn(Optional.of(pet));

    // when & then
    assertThrows(MeongnyangerangException.class, () ->
        userPetService.deletePet(userId, petId));
  }
}
