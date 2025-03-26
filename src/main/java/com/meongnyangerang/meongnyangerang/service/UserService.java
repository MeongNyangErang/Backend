package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.DUPLICATE_EMAIL;

import com.meongnyangerang.meongnyangerang.component.MailComponent;
import com.meongnyangerang.meongnyangerang.domain.auth.AuthenticationCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.AuthenticationCodeRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final AuthenticationCodeRepository authenticationCodeRepository;
  private final MailComponent mailComponent;

  // 인증 코드 발송
  public void sendVerificationCode(String email) {

    // 중복 가입 방지
    if (userRepository.existsByEmail(email)) {
      throw new MeongnyangerangException(DUPLICATE_EMAIL);
    }

    // 6자리 랜덤 숫자 생성(Math.random()은 예측 가능한 난수 생성 → 보안 취약 → SecureRandom 사용)
    int code = 100_000 + new SecureRandom().nextInt(900_000);

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
}
