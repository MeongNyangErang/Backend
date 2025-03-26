package com.meongnyangerang.meongnyangerang.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.component.MailComponent;
import com.meongnyangerang.meongnyangerang.domain.auth.AuthenticationCode;
import com.meongnyangerang.meongnyangerang.repository.AuthenticationCodeRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
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
  private AuthenticationCodeRepository authenticationCodeRepository;

  @Mock
  private MailComponent mailComponent;

  @Test
  @DisplayName("인증 코드 생성 및 저장 테스트")
  void sendVerificationCode_Success() {
    // given
    String email = "test@example.com";
    when(userRepository.existsByEmail(email)).thenReturn(false);

    // when
    authService.sendVerificationCode(email);

    // then
    verify(authenticationCodeRepository, times(1)).deleteAllByEmail(email);
    verify(authenticationCodeRepository, times(1)).save(any(AuthenticationCode.class));
    verify(mailComponent, times(1)).sendMail(eq(email), anyString(), anyString());
  }
}
