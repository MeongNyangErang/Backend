package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.*;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXIST_ACCOUNT;

import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.UserPet;
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

  @Transactional
  public void registerPet(Long userId, UserPetRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    // 최대 10마리 제한
    long count = userPetRepository.countByUserId(userId);
    if (count >= 10) {
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
}
