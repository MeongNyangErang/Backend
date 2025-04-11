package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.component.MailComponent;
import com.meongnyangerang.meongnyangerang.domain.auth.AuthenticationCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.AuthenticationCodeRepository;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @InjectMocks
  private AuthService authService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private HostRepository hostRepository;

  @Mock
  private AuthenticationCodeRepository authenticationCodeRepository;

  @Mock
  private MailComponent mailComponent;

  @Test
  @DisplayName("인증 코드 생성 및 이메일 발송 테스트")
  void sendVerificationCode_Success() {
    // given
    String email = "test@example.com";

    // when
    authService.sendVerificationCode(email);

    // then
    verify(authenticationCodeRepository).deleteAllByEmail(email);
    verify(authenticationCodeRepository).save(argThat(saved -> saved.getEmail().equals(email)));
    verify(mailComponent).sendMail(eq(email), eq("회원가입 인증코드"), contains("인증코드:"));
  }

  @Test
  @DisplayName("인증 코드 정상 검증 테스트")
  void verifyCode_Success() {
    // given
    String email = "test@example.com";
    String code = "123456";
    AuthenticationCode authCode = AuthenticationCode.builder()
        .email(email)
        .code(code)
        .createdAt(LocalDateTime.now())
        .build();

    when(authenticationCodeRepository.findByEmail(email)).thenReturn(Optional.of(authCode));

    // when
    authService.verifyCode(email, code);

    // then
    verify(authenticationCodeRepository, times(1)).delete(authCode);
  }

  @Test
  @DisplayName("인증 코드 검증 실패 테스트")
  void verifyCode_Fail_InvalidCode() {
    // given
    String email = "test@example.com";
    String code = "123456";

    when(authenticationCodeRepository.findByEmail(email)).thenReturn(Optional.of(AuthenticationCode.builder()
        .email(email)
        .code(code)
        .createdAt(LocalDateTime.now())
        .build()));

    // when & then
    assertThrows(MeongnyangerangException.class, () -> authService.verifyCode(email, "654321"));
  }
}
