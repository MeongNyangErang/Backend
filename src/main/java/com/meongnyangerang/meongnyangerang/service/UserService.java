package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.domain.user.Role.*;
import static com.meongnyangerang.meongnyangerang.domain.user.UserStatus.*;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.*;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.DUPLICATE_EMAIL;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.USER_ALREADY_EXISTS;

import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.UserStatus;
import com.meongnyangerang.meongnyangerang.dto.LoginRequest;
import com.meongnyangerang.meongnyangerang.dto.UserSignupRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.jwt.JwtTokenProvider;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final HostRepository hostRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  // 사용자 회원가입
  public void registerUser(UserSignupRequest request) {

    // 중복 가입 확인
    if (userRepository.existsByEmail(request.getEmail()) ||
        hostRepository.existsByEmail(request.getEmail())) {
      throw new MeongnyangerangException(DUPLICATE_EMAIL);
    }

    // 유저 저장(바로 role 부여)
    userRepository.save(User.builder()
        .email(request.getEmail())
        .nickname(request.getNickname())
        .password(passwordEncoder.encode(request.getPassword()))
        .profileImage(request.getProfileImage())
        .status(ACTIVE)
        .role(ROLE_USER)
        .build());
  }

  // 사용자 로그인
  public String login(@Valid LoginRequest request) {

    // 사용자 조회
    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    // 비밀번호 검증
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new MeongnyangerangException(INVALID_PASSWORD);
    }

    // 상태 검증은 JwtTokenProvider 내부에서 수행됨

    return jwtTokenProvider.createToken(user.getId(), user.getEmail(), user.getRole().name(),
        user.getStatus());
  }
}
