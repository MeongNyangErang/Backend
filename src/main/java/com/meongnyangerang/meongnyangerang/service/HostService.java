package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.domain.host.HostStatus.PENDING;
import static com.meongnyangerang.meongnyangerang.domain.user.Role.ROLE_HOST;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.DUPLICATE_EMAIL;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.INVALID_PASSWORD;

import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.host.HostStatus;
import com.meongnyangerang.meongnyangerang.dto.HostSignupRequest;
import com.meongnyangerang.meongnyangerang.dto.LoginRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.jwt.JwtTokenProvider;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HostService {

  private final HostRepository hostRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  // 호스트 회원가입
  public void registerHost(HostSignupRequest request) {

    // 호스트 중복 가입 방지
    if (hostRepository.existsByEmail(request.getEmail())) {
      throw new MeongnyangerangException(DUPLICATE_EMAIL);
    }

    // 호스트 정보 저장(호스트는 처음 가입할때 pending 상태이고, 나중에 관리자가 역할 및 상태를 부여)
    hostRepository.save(Host.builder()
        .email(request.getEmail())
        .name(request.getName())
        .nickname(request.getNickname())
        .password(passwordEncoder.encode(request.getPassword()))
        .profileImageUrl(request.getProfileImageUrl())
        .businessLicenseImageUrl(request.getBusinessLicenseImageUrl())
        .submitDocumentImageUrl(request.getSubmitDocumentImageUrl())
        .phoneNumber(request.getPhoneNumber())
        .status(PENDING) // 기본적으로 대기 상태
        .role(ROLE_HOST)
        .build());
  }

  // 호스트 로그인
  public String login(@Valid LoginRequest request) {

    // 호스트 조회
    Host host = hostRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.NOT_EXIST_ACCOUNT));

    // 비밀번호 검증
    if (!passwordEncoder.matches(request.getPassword(), host.getPassword())) {
      throw new MeongnyangerangException(INVALID_PASSWORD);
    }

    // 호스트 상태 검증

    if (host.getStatus() == HostStatus.DELETED) {
      throw new MeongnyangerangException(ErrorCode.ACCOUNT_DELETED);
    }

    if (host.getStatus() == HostStatus.PENDING) {
      throw new MeongnyangerangException(ErrorCode.ACCOUNT_PENDING);
    }

    return jwtTokenProvider.createToken(host.getId(), host.getEmail(), host.getRole().name(),
        host.getStatus());
  }
}
