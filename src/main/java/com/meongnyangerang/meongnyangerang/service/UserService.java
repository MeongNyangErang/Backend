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
  private final AuthenticationCodeRepository authenticationCodeRepository;
  private final MailComponent mailComponent;

  // 인증 코드 발송
  @Transactional
  public void sendVerificationCode(String email) {

    // 중복 가입 방지
    if (userRepository.existsByEmail(email)) {
      throw new MeongnyangerangException(DUPLICATE_EMAIL);
    }

    // 6자리 랜덤 숫자 생성(Math.random()은 예측 가능한 난수 생성 → 보안 취약 → SecureRandom 사용)
    String code = String.format("%06d", new SecureRandom().nextInt(900_000));

    // 기존 코드 삭제
    authenticationCodeRepository.deleteAllByEmail(email);

    authenticationCodeRepository.save(AuthenticationCode.builder()
        .email(email)
        .code(code)
        .build());

    // 이메일 발송
    String subject = "회원가입 인증코드";
    String text = "인증코드: " + code + "\n3분 안에 입력해주세요.";
    mailComponent.sendMail(email, subject, text);
  }

  // 인증 코드 검증 (3분 이내인지 체크)
  @Transactional
  public void verifyCode(String email, String code) {
    AuthenticationCode authCode = authenticationCodeRepository.findByEmail(email)
        .orElseThrow(() -> new MeongnyangerangException(AUTH_CODE_NOT_FOUND));

    // 3분 초과 체크
    if (authCode.getCreatedAt().plusMinutes(3).isBefore(LocalDateTime.now())) {
      throw new MeongnyangerangException(EXPIRED_AUTH_CODE);
    }

    // 코드 일치 여부 확인
    if (!authCode.getCode().equals(code)) {
      throw new MeongnyangerangException(INVALID_AUTH_CODE);
    }

    // 코드 사용 후 무효화 (삭제)
    authenticationCodeRepository.delete(authCode);
  }

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
