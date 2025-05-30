package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.domain.host.HostStatus.PENDING;
import static com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus.RESERVED;
import static com.meongnyangerang.meongnyangerang.domain.user.Role.ROLE_HOST;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ACCOUNT_DELETED;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ACCOUNT_PENDING;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ALREADY_REGISTERED_NAME;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ALREADY_REGISTERED_NICKNAME;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ALREADY_REGISTERED_PHONE_NUMBER;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.DUPLICATE_EMAIL;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.DUPLICATE_PHONE_NUMBER;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.FILE_IS_EMPTY;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.INVALID_AUTHORIZED;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.INVALID_PASSWORD;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXIST_ACCOUNT;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.RESERVED_RESERVATION_EXISTS;

import com.meongnyangerang.meongnyangerang.domain.auth.RefreshToken;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.host.HostStatus;
import com.meongnyangerang.meongnyangerang.domain.user.AuthProvider;
import com.meongnyangerang.meongnyangerang.dto.HostProfileResponse;
import com.meongnyangerang.meongnyangerang.dto.HostSignupRequest;
import com.meongnyangerang.meongnyangerang.dto.LoginRequest;
import com.meongnyangerang.meongnyangerang.dto.LoginResponse;
import com.meongnyangerang.meongnyangerang.dto.PasswordUpdateRequest;
import com.meongnyangerang.meongnyangerang.dto.auth.KakaoUserInfoResponse;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.jwt.JwtTokenProvider;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
import com.meongnyangerang.meongnyangerang.repository.ReservationRepository;
import com.meongnyangerang.meongnyangerang.repository.auth.RefreshTokenRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class HostService {

  private final HostRepository hostRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final ImageService imageService;
  private final AuthService authService;
  private final ReservationRepository reservationRepository;
  private final RefreshTokenRepository refreshTokenRepository;

  // 호스트 회원가입
  public void registerHost(HostSignupRequest request,
      MultipartFile profileImage, MultipartFile businessLicenseImage,
      MultipartFile submitDocumentImage) {

    // 호스트 중복 가입 방지
    if (hostRepository.existsByEmail(request.getEmail())) {
      throw new MeongnyangerangException(DUPLICATE_EMAIL);
    }

    // 필수 이미지 누락 시 예외
    if (businessLicenseImage == null || submitDocumentImage == null) {
      throw new MeongnyangerangException(FILE_IS_EMPTY);
    }

    String profileImageUrl = profileImage != null ? imageService.storeImage(profileImage) : null;
    String businessImageUrl = imageService.storeImage(businessLicenseImage);
    String submitImageUrl = imageService.storeImage(submitDocumentImage);

    // 호스트 정보 저장(호스트는 처음 가입할때 pending 상태이고, 나중에 관리자가 역할 및 상태를 부여)
    hostRepository.save(Host.builder()
        .email(request.getEmail())
        .name(request.getName())
        .nickname(request.getNickname())
        .password(passwordEncoder.encode(request.getPassword()))
        .profileImageUrl(profileImageUrl)
        .businessLicenseImageUrl(businessImageUrl)
        .submitDocumentImageUrl(submitImageUrl)
        .phoneNumber(request.getPhoneNumber())
        .status(PENDING) // 기본적으로 대기 상태
        .role(ROLE_HOST)
        .build());
  }

  // 호스트 로그인
  @Transactional
  public LoginResponse login(@Valid LoginRequest request) {

    // 호스트 조회
    Host host = hostRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    // 비밀번호 검증
    if (!passwordEncoder.matches(request.getPassword(), host.getPassword())) {
      throw new MeongnyangerangException(INVALID_PASSWORD);
    }

    // 호스트 상태 검증

    if (host.getStatus() == HostStatus.DELETED) {
      throw new MeongnyangerangException(ACCOUNT_DELETED);
    }

    if (host.getStatus() == HostStatus.PENDING) {
      throw new MeongnyangerangException(ACCOUNT_PENDING);
    }

    // 액세스 토큰 + 리프레시 토큰 발급
    return issueJwtToken(host);
  }

  // 호스트 카카오 로그인
  @Transactional
  public LoginResponse loginWithKakao(KakaoUserInfoResponse kakaoUser) {
    String email = kakaoUser.email();

    Host host = hostRepository.findByEmail(email)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    if (host.getStatus() == HostStatus.PENDING) {
      throw new MeongnyangerangException(ACCOUNT_PENDING);
    }

    if (host.getStatus() == HostStatus.DELETED) {
      throw new MeongnyangerangException(ACCOUNT_DELETED);
    }

    return issueJwtToken(host);
  }

  // Access Token + Refresh Token 발급 메서드
  private LoginResponse issueJwtToken(Host host) {

    // Access Token 발급
    String accessToken = jwtTokenProvider.createAccessToken(host.getId(), host.getEmail(),
        host.getRole().name(), host.getStatus());

    // Refresh Token 발급
    String refreshToken = jwtTokenProvider.createRefreshToken();

    // Refresh Token 저장
    refreshTokenRepository.deleteByUserIdAndRole(host.getId(), host.getRole()); // 중복 방지
    refreshTokenRepository.save(RefreshToken.builder()
        .refreshToken(refreshToken)
        .userId(host.getId())
        .role(host.getRole())
        .expiryDate(LocalDateTime.now().plusDays(7))
        .build());

    // Access Token + Refresh Token 함께 응답
    return new LoginResponse(accessToken, refreshToken);
  }

  // 호스트 회원 탈퇴
  @Transactional
  public void deleteHost(Long hostId) {

    Host host = hostRepository.findById(hostId)
        .orElseThrow(() -> new MeongnyangerangException(ErrorCode.NOT_EXIST_ACCOUNT));

    // 호스트가 등록한 숙소 중 예약 상태가 RESERVED인 것이 있으면 탈퇴 불가
    if (reservationRepository.existsByHostIdAndStatus(hostId, RESERVED)) {
      throw new MeongnyangerangException(RESERVED_RESERVATION_EXISTS);
    }

    host.setStatus(HostStatus.DELETED);
    host.setDeletedAt(LocalDateTime.now());
  }

  // 호스트 프로필 조회
  @Transactional(readOnly = true)
  public HostProfileResponse getHostProfile(Long hostId) {
    Host host = hostRepository.findById(hostId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    return HostProfileResponse.of(host);
  }

  // 호스트 전화번호 변경
  @Transactional
  public void updatePhoneNumber(Long hostId, String newPhoneNumber) {
    Host host = hostRepository.findById(hostId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    // 바꾸려는 전화번호가 기존의 전화번호와 같을 시 예외처리
    if (host.getPhoneNumber().equals(newPhoneNumber)) {
      throw new MeongnyangerangException(ALREADY_REGISTERED_PHONE_NUMBER);
    }

    // 다른 호스트가 이미 해당 전화번호를 사용중일 시 예외처리
    if (hostRepository.existsByPhoneNumberAndIdNot(newPhoneNumber, hostId)) {
      throw new MeongnyangerangException(DUPLICATE_PHONE_NUMBER);
    }
    host.updatePhoneNumber(newPhoneNumber);
  }

  // 호스트 이름 변경
  public void updateName(Long hostId, String newName) {
    Host host = hostRepository.findById(hostId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    if (host.getName().equals(newName)) {
      throw new MeongnyangerangException(ALREADY_REGISTERED_NAME);
    }

    host.updateName(newName);
  }

  // 호스트 비밀번호 변경
  public void updatePassword(Long hostId, PasswordUpdateRequest request) {
    Host host = hostRepository.findById(hostId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    if (!passwordEncoder.matches(request.currentPassword(), host.getPassword())) {
      throw new MeongnyangerangException(INVALID_PASSWORD);
    }
    host.updatePassword(passwordEncoder.encode(request.newPassword()));
  }

  // 호스트 닉네임 변경
  @Transactional
  public void updateNickname(Long hostId, String newNickname) {
    Host host = hostRepository.findById(hostId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    if (host.getNickname().equals(newNickname)) {
      throw new MeongnyangerangException(ALREADY_REGISTERED_NICKNAME);
    }

    // 이메일 중복 확인 및 예외처리
    authService.checkNickname(newNickname);

    host.updateNickname(newNickname);
  }

  // 호스트 프로필 사진 변경
  @Transactional
  public void updateProfileImage(Long hotsId, MultipartFile newProfileImage) {
    Host host = hostRepository.findById(hotsId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    if (host.getProfileImageUrl() != null && !host.getProfileImageUrl().isBlank()) {
      imageService.deleteImageAsync(host.getProfileImageUrl());
    }

    host.updateProfileImage(imageService.storeImage(newProfileImage));
  }
}
