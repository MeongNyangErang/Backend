package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.DUPLICATE_EMAIL;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.USER_ALREADY_EXISTS;

import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.UserStatus;
import com.meongnyangerang.meongnyangerang.dto.UserSignupRequest;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final HostRepository hostRepository;
  private final PasswordEncoder passwordEncoder;

  // 사용자 회원가입
  public void registerUser(UserSignupRequest request) {

    // 중복 가입 확인
    if (userRepository.existsByEmail(request.getEmail()) ||
        hostRepository.existsByEmail(request.getEmail())) {
      throw new MeongnyangerangException(DUPLICATE_EMAIL);
    }

    // 유저 저장
    userRepository.save(User.builder()
        .email(request.getEmail())
        .nickname(request.getNickname())
        .password(passwordEncoder.encode(request.getPassword()))
        .profileImage(request.getProfileImage())
        .status(UserStatus.ACTIVE)
        .build());
  }
}
