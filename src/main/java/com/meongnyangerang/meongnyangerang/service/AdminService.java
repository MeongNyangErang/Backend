package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.INVALID_PASSWORD;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXIST_ACCOUNT;

import com.meongnyangerang.meongnyangerang.domain.admin.Admin;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.host.HostStatus;
import com.meongnyangerang.meongnyangerang.dto.CustomApplicationResponse;
import com.meongnyangerang.meongnyangerang.dto.LoginRequest;
import com.meongnyangerang.meongnyangerang.dto.PendingHostDetailResponse;
import com.meongnyangerang.meongnyangerang.dto.PendingHostListResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.jwt.JwtTokenProvider;
import com.meongnyangerang.meongnyangerang.repository.AdminRepository;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import jakarta.validation.Valid;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

  private final AdminRepository adminRepository;
  private final HostRepository hostRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  // 관리자 로그인
  public String login(@Valid LoginRequest request) {

    // 관리자 조회
    Admin admin = adminRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    // 비밀번호 검증
    if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
      throw new MeongnyangerangException(INVALID_PASSWORD);
    }

    // 상태 검증은 JwtTokenProvider 내부에서 수행됨

    return jwtTokenProvider.createToken(admin.getId(), admin.getEmail(), admin.getRole().name(),
        admin.getStatus());
  }

  // 가입 신청 목록 조회
  public CustomApplicationResponse getPendingHostList(Long cursorId, int size) {
    List<Host> pendingHosts = hostRepository.findAllByStatus(cursorId, size + 1,
        HostStatus.PENDING.name());

    List<PendingHostListResponse> content = pendingHosts.stream().limit(size)
        .map(this::mapToPendingHostList).toList();

    boolean hasNext = pendingHosts.size() > size;
    Long cursor = hasNext ? pendingHosts.get(size).getId() : null;

    return new CustomApplicationResponse(content, cursor, hasNext);
  }

  private PendingHostListResponse mapToPendingHostList(Host host) {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    return PendingHostListResponse.builder()
        .hostId(host.getId())
        .createdAt(host.getCreatedAt().format(dateFormatter))
        .build();
  }

  // 가입 신청 상세 조회
  public PendingHostDetailResponse getPendingHostDetail(Long hostId) {
    // 호스트 조회 (PENDING 상태가 아닌 경우 예외 발생)
    Host host = hostRepository.findByIdAndStatus(hostId, HostStatus.PENDING)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.HOST_ALREADY_PROCESSED));

    return PendingHostDetailResponse.builder()
        .email(host.getEmail())
        .name(host.getName())
        .phoneNumber(host.getPhoneNumber())
        .businessLicenseImageUrl(host.getBusinessLicenseImageUrl())
        .submitDocumentImageUrl(host.getSubmitDocumentImageUrl())
        .build();
  }
}
