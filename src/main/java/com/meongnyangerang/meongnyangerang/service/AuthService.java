package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.*;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.AUTH_CODE_NOT_FOUND;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.DUPLICATE_EMAIL;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.DUPLICATE_NICKNAME;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.EXPIRED_AUTH_CODE;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.INVALID_AUTH_CODE;

import com.meongnyangerang.meongnyangerang.component.MailComponent;
import com.meongnyangerang.meongnyangerang.domain.admin.Admin;
import com.meongnyangerang.meongnyangerang.domain.auth.AuthenticationCode;
import com.meongnyangerang.meongnyangerang.domain.auth.RefreshToken;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.host.HostStatus;
import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.UserStatus;
import com.meongnyangerang.meongnyangerang.dto.auth.RefreshResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.jwt.JwtTokenProvider;
import com.meongnyangerang.meongnyangerang.repository.AdminRepository;
import com.meongnyangerang.meongnyangerang.repository.AuthenticationCodeRepository;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.repository.auth.RefreshTokenRepository;
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
  private final AdminRepository adminRepository;
  private final AuthenticationCodeRepository authenticationCodeRepository;
  private final MailComponent mailComponent;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;

  // 인증 코드 발송
  @Transactional
  public void sendVerificationCode(String email) {

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

  // 사용자 이메일 중복 확인
  public void checkUserEmail(String email) {
    if (userRepository.existsByEmail(email)) {
      throw new MeongnyangerangException(DUPLICATE_EMAIL);
    }
  }

  // 호스트 이메일 중복 확인
  public void checkHostEmail(String email) {
    if (hostRepository.existsByEmail(email)) {
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

  // 리프레시 토큰을 이용해 새로운 액세스 토큰을 재발급
  public RefreshResponse reissueAccessToken(String refreshToken) {

    // 토큰 유효성 검증
    jwtTokenProvider.validateToken(refreshToken);

    // DB 조회
    RefreshToken tokenEntity = refreshTokenRepository.findByRefreshToken(refreshToken)
        .orElseThrow(() -> new MeongnyangerangException(INVALID_REFRESH_TOKEN));

    // 만료 시간 체크
    if (tokenEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
      throw new MeongnyangerangException(EXPIRED_REFRESH_TOKEN);
    }

    // 사용자 정보 조회 및 상태 확인 → AccessToken 재발급
    Long userId = tokenEntity.getUserId();
    Role role = tokenEntity.getRole();

    // 새로운 Access Token을 RefreshResponse로 감싸서 반환
    return new RefreshResponse(switch (role) {
      case ROLE_USER -> {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));
        if (user.getStatus() == UserStatus.DELETED) {
          throw new MeongnyangerangException(ACCOUNT_DELETED);
        }
        yield jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), role.name(), user.getStatus());
      }
      case ROLE_HOST -> {
        Host host = hostRepository.findById(userId)
            .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));
        if (host.getStatus() == HostStatus.DELETED || host.getStatus() == HostStatus.PENDING) {
          throw new MeongnyangerangException(INVALID_AUTHORIZED);
        }
        yield jwtTokenProvider.createAccessToken(host.getId(), host.getEmail(), role.name(), host.getStatus());
      }
      case ROLE_ADMIN -> {
        Admin admin = adminRepository.findById(userId)
            .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));
        yield jwtTokenProvider.createAccessToken(admin.getId(), admin.getEmail(), role.name(), admin.getStatus());
      }
    });
  }
}
