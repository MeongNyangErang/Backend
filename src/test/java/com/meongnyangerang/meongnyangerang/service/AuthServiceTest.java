package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.AUTH_CODE_NOT_FOUND;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.DUPLICATE_EMAIL;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.DUPLICATE_NICKNAME;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.EXPIRED_AUTH_CODE;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.INVALID_AUTH_CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.component.MailComponent;
import com.meongnyangerang.meongnyangerang.domain.auth.AuthenticationCode;
import com.meongnyangerang.meongnyangerang.domain.auth.RefreshToken;
import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.UserStatus;
import com.meongnyangerang.meongnyangerang.dto.auth.RefreshResponse;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.jwt.JwtTokenProvider;
import com.meongnyangerang.meongnyangerang.repository.AdminRepository;
import com.meongnyangerang.meongnyangerang.repository.AuthenticationCodeRepository;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.repository.auth.RefreshTokenRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
  private AdminRepository adminRepository;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

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
  @DisplayName("인증 코드 검증 실패 - 코드 불일치")
  void verifyCode_Fail_InvalidCode() {
    // given
    String email = "test@example.com";
    String code = "123456";
    String wrongCode = "654321";
    AuthenticationCode authCode = AuthenticationCode.builder()
        .email(email)
        .code(code)
        .createdAt(LocalDateTime.now())
        .build();

    when(authenticationCodeRepository.findByEmail(email)).thenReturn(Optional.of(authCode));

    // when & then
    MeongnyangerangException ex = assertThrows(MeongnyangerangException.class,
        () -> authService.verifyCode(email, wrongCode));
    assertEquals(INVALID_AUTH_CODE, ex.getErrorCode());
  }

  @Test
  @DisplayName("인증 코드 검증 실패 - 인증 코드 없음")
  void verifyCode_Fail_NoCode() {
    // given
    String email = "test@example.com";
    when(authenticationCodeRepository.findByEmail(email)).thenReturn(Optional.empty());

    // when & then
    MeongnyangerangException ex = assertThrows(MeongnyangerangException.class,
        () -> authService.verifyCode(email, "123456"));
    assertEquals(AUTH_CODE_NOT_FOUND, ex.getErrorCode());
  }

  @Test
  @DisplayName("인증 코드 검증 실패 - 3분 초과")
  void verifyCode_Fail_ExpiredCode() {
    // given
    String email = "test@example.com";
    String code = "123456";
    AuthenticationCode authCode = AuthenticationCode.builder()
        .email(email)
        .code(code)
        .createdAt(LocalDateTime.now().minusMinutes(4))
        .build();

    when(authenticationCodeRepository.findByEmail(email)).thenReturn(Optional.of(authCode));

    // when & then
    MeongnyangerangException ex = assertThrows(MeongnyangerangException.class,
        () -> authService.verifyCode(email, code));
    assertEquals(EXPIRED_AUTH_CODE, ex.getErrorCode());
  }

  @Test
  @DisplayName("사용자 이메일 중복 - 예외 발생")
  void checkUserEmail_Duplicated() {
    // given
    String email = "duplicate@user.com";
    when(userRepository.existsByEmail(email)).thenReturn(true);

    // when & then
    MeongnyangerangException ex = assertThrows(MeongnyangerangException.class,
        () -> authService.checkUserEmail(email));
    assertEquals(DUPLICATE_EMAIL, ex.getErrorCode());
  }

  @Test
  @DisplayName("호스트 이메일 중복 - 예외 발생")
  void checkHostEmail_Duplicated() {
    // given
    String email = "duplicate@host.com";
    when(hostRepository.existsByEmail(email)).thenReturn(true);

    // when & then
    MeongnyangerangException ex = assertThrows(MeongnyangerangException.class,
        () -> authService.checkHostEmail(email));
    assertEquals(DUPLICATE_EMAIL, ex.getErrorCode());
  }

  @Test
  @DisplayName("닉네임 중복 - 사용자 or 호스트 존재")
  void checkNickname_Duplicated() {
    // given
    String nickname = "tester";
    when(userRepository.existsByNickname(nickname)).thenReturn(false);
    when(hostRepository.existsByNickname(nickname)).thenReturn(true);

    // when & then
    MeongnyangerangException ex = assertThrows(MeongnyangerangException.class,
        () -> authService.checkNickname(nickname));
    assertEquals(DUPLICATE_NICKNAME, ex.getErrorCode());
  }

  @Test
  @DisplayName("정상적인 유저의 리프레시 토큰으로 액세스 토큰 재발급 성공")
  void should_reissue_access_token_when_valid_refresh_token_for_user() {
    // given
    String refreshToken = "valid-refresh-token";
    Long userId = 1L;
    Role role = Role.ROLE_USER;
    UserStatus status = UserStatus.ACTIVE;

    RefreshToken token = RefreshToken.builder()
        .refreshToken(refreshToken)
        .userId(userId)
        .role(role)
        .expiryDate(LocalDateTime.now().plusDays(1))
        .build();

    User user = User.builder()
        .id(userId)
        .email("test@example.com")
        .status(status)
        .role(role)
        .build();

    String newAccessToken = "new-access-token";

    // when
    Mockito.when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
    Mockito.when(refreshTokenRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.of(token));
    Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    Mockito.when(jwtTokenProvider.createAccessToken(userId, user.getEmail(), role.name(), status)).thenReturn(newAccessToken);

    // then
    RefreshResponse response = authService.reissueAccessToken(refreshToken);
    assertThat(response.accessToken()).isEqualTo(newAccessToken);
  }
}
