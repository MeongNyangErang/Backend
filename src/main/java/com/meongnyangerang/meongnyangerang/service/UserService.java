package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.AUTH_CODE_NOT_FOUND;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.DUPLICATE_EMAIL;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.EXPIRED_AUTH_CODE;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.INVALID_AUTH_CODE;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.USER_ALREADY_EXISTS;

import com.meongnyangerang.meongnyangerang.component.MailComponent;
import com.meongnyangerang.meongnyangerang.domain.auth.AuthenticationCode;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.UserStatus;
import com.meongnyangerang.meongnyangerang.dto.UserSignupRequest;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.AuthenticationCodeRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  // 사용자 회원가입
  public void registerUser(UserSignupRequest request) {

    // 중복 가입 확인
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new MeongnyangerangException(USER_ALREADY_EXISTS);
    }

    // 유저 저장
    userRepository.save(User.builder()
        .email(request.getEmail())
        .nickname(request.getNickname())
        .password(request.getPassword()) // 추후 BCrypt.hashpw 를 사용하여 비밀번호 암호화 예정
        .profileImage(request.getProfileImage())
        .status(UserStatus.ACTIVE)
        .build());
  }
}
