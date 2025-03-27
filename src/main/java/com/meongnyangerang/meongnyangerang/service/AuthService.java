package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.AUTH_CODE_NOT_FOUND;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.DUPLICATE_EMAIL;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.DUPLICATE_NICKNAME;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.EXPIRED_AUTH_CODE;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.INVALID_AUTH_CODE;

import com.meongnyangerang.meongnyangerang.component.MailComponent;
import com.meongnyangerang.meongnyangerang.domain.auth.AuthenticationCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.AuthenticationCodeRepository;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final HostRepository hostRepository;
  private final AuthenticationCodeRepository authenticationCodeRepository;
  private final MailComponent mailComponent;

  // 인증 코드 발송
  @Transactional
  public void sendVerificationCode(String email) {

    // 이메일 중복 검증 메서드
    checkEmail(email);

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

  // 이메일 중복 확인
  public void checkEmail(String email) {

    // 사용자, 호스트 이메일 중복 동시 체크
    if (userRepository.existsByEmail(email) || hostRepository.existsByEmail(email)) {
      throw new MeongnyangerangException(DUPLICATE_EMAIL);
    }
  }

  // 닉네임 중복 확인
  public void checkNickname(String nickname) {

    // 사용자, 호스트 이메일 중복 동시 체크
    if (userRepository.existsByNickname(nickname) || hostRepository.existsByNickname(nickname)) {
      throw new MeongnyangerangException(DUPLICATE_NICKNAME);
    }
  }
}
