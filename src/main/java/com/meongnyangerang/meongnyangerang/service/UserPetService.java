package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.*;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXIST_ACCOUNT;

import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.UserPet;
import com.meongnyangerang.meongnyangerang.dto.UserPetRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.UserPetRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPetService {

  private final UserRepository userRepository;
  private final UserPetRepository userPetRepository;

  // 반려동물 등록
  @Transactional
  public void registerPet(Long userId, UserPetRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    // 최대 10마리 제한
    if (userPetRepository.countByUserId(userId) >= 10) {
      throw new MeongnyangerangException(MAX_PET_COUNT_EXCEEDED);
    }

    UserPet userPet = UserPet.builder()
        .user(user)
        .name(request.getName())
        .birthDate(request.getBirthDate())
        .type(request.getType())
        .personality(request.getPersonality())
        .activityLevel(request.getActivityLevel())
        .build();

    userPetRepository.save(userPet);
  }

  // 반려동물 수정
  @Transactional
  public void updatePet(Long userId, Long petId, UserPetRequest request) {
    UserPet userPet = userPetRepository.findById(petId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_PET));

    // 본인의 반려동물인지 확인
    if (!userPet.getUser().getId().equals(userId)) {
      throw new MeongnyangerangException(INVALID_AUTHORIZED);
    }

    userPet.update(request);
  }

  // 반려동물 삭제
  @Transactional
  public void deletePet(Long userId, Long petId) {
    UserPet userPet = userPetRepository.findById(petId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_PET));

    // 본인의 반려동물인지 확인
    if (!userPet.getUser().getId().equals(userId)) {
      throw new MeongnyangerangException(INVALID_AUTHORIZED);
    }

    userPetRepository.delete(userPet);
  }

  // 반려동물 조회
  @Transactional(readOnly = true)
  public List<UserPetResponse> getUserPets(Long userId) {
    return userPetRepository.findAllByUserId(userId).stream()
        .map(UserPetResponse::from)
        .collect(Collectors.toList());
  }
}
