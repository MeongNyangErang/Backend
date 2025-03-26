package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.DUPLICATE_EMAIL;

import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.host.HostStatus;
import com.meongnyangerang.meongnyangerang.dto.HostSignupRequest;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.AuthenticationCodeRepository;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HostService {

  private final HostRepository hostRepository;
  private final AuthenticationCodeRepository authenticationCodeRepository;

  // 호스트 회원가입
  public void registerHost(HostSignupRequest request) {

    // 중복 가입 방지
    if (hostRepository.existsByEmail(request.getEmail())) {
      throw new MeongnyangerangException(DUPLICATE_EMAIL);
    }

    // 호스트 정보 저장
    hostRepository.save(Host.builder()
        .email(request.getEmail())
        .name(request.getName())
        .nickname(request.getNickname())
        .password(request.getPassword())
        .profileImageUrl(request.getProfileImageUrl())
        .businessLicenseImageUrl(request.getBusinessLicenseImageUrl())
        .submitDocumentImageUrl(request.getSubmitDocumentImageUrl())
        .phoneNumber(request.getPhoneNumber())
        .status(HostStatus.PENDING) // 기본적으로 대기 상태
        .build());
  }
}
