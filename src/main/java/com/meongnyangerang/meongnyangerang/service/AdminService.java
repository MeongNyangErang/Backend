package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.INVALID_PASSWORD;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXIST_ACCOUNT;

import com.meongnyangerang.meongnyangerang.component.MailComponent;
import com.meongnyangerang.meongnyangerang.domain.admin.Admin;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.host.HostStatus;
import com.meongnyangerang.meongnyangerang.domain.user.Role;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

  private final AdminRepository adminRepository;
  private final HostRepository hostRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final MailComponent mailComponent;

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

  // 호스트 가입 승인
  @Transactional
  public void approveHost(Long hostId) {
    // 호스트 조회
    Host host = hostRepository.findById(hostId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.NOT_EXISTS_HOST));

    // 호스트의 상태가 PENDING이 아닌 경우 예외 처리
    if (host.getStatus() != HostStatus.PENDING) {
      throw new MeongnyangerangException(ErrorCode.HOST_ALREADY_PROCESSED);
    }

    // 호스트 상태 변경
    host.setStatus(HostStatus.ACTIVE);

    // 가입 승인 이메일 발송
    mailComponent.sendMail(
        host.getEmail(),
        "[멍냥이랑] 요청하신 호스트 가입이 승인되었습니다!",
        """
            <div>
              <h2>안녕하세요, 멍냥이랑입니다.</h2>
              <p>요청하신 <strong>호스트 가입</strong>이 승인되었습니다!</p>
              <p>앞으로 좋은 서비스로 보답하겠습니다.</p>
              <p>감사합니다.</p>
            </div>
            """
    );
  }

  // 호스트 가입 거절
  @Transactional
  public void rejectHost(Long hostId) {
    // 호스트 조회
    Host host = hostRepository.findById(hostId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.NOT_EXISTS_HOST));

    // 호스트의 상태가 PENDING이 아닌 경우 예외 처리
    if (host.getStatus() != HostStatus.PENDING) {
      throw new MeongnyangerangException(ErrorCode.HOST_ALREADY_PROCESSED);
    }

    // 가입 승인 이메일 발송
    mailComponent.sendMail(
        host.getEmail(),
        "[멍냥이랑] 요청하신 호스트 가입이 거절되었습니다",
        """
            <div>
              <h2>안녕하세요, 멍냥이랑입니다.</h2>
              <p>요청하신 <strong>호스트 가입</strong>이 거절되었습니다.</p>
              <p>제출하신 서류를 다시 검토해주시고, 가입 신청 부탁드립니다.</p>
              <p>감사합니다.</p>
            </div>
            """
    );

    // DB에서 호스트 정보 삭제
    hostRepository.delete(host);
  }
}
