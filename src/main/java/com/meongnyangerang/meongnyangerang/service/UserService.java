package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.domain.user.Role.ROLE_USER;
import static com.meongnyangerang.meongnyangerang.domain.user.UserStatus.ACTIVE;
import static com.meongnyangerang.meongnyangerang.domain.user.UserStatus.DELETED;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ACCOUNT_DELETED;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ALREADY_REGISTERED_NICKNAME;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.DUPLICATE_EMAIL;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.INVALID_PASSWORD;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXIST_ACCOUNT;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.RESERVED_RESERVATION_EXISTS;

import com.meongnyangerang.meongnyangerang.domain.auth.RefreshToken;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.LoginRequest;
import com.meongnyangerang.meongnyangerang.dto.LoginResponse;
import com.meongnyangerang.meongnyangerang.dto.PasswordUpdateRequest;
import com.meongnyangerang.meongnyangerang.dto.UserProfileResponse;
import com.meongnyangerang.meongnyangerang.dto.UserSignupRequest;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.jwt.JwtTokenProvider;
import com.meongnyangerang.meongnyangerang.repository.ReservationRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
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
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final ImageService imageService;
  private final AuthService authService;
  private final ReservationRepository reservationRepository;
  private final RefreshTokenRepository refreshTokenRepository;

  // 사용자 회원가입
  public void registerUser(UserSignupRequest request, MultipartFile profileImage) {

    // 사용자 중복 가입 방지
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new MeongnyangerangException(DUPLICATE_EMAIL);
    }

    // 프로필 이미지 업로드 (선택적)
    String profileImageUrl = null;
    if (profileImage != null && !profileImage.isEmpty()) {
      profileImageUrl = imageService.storeImage(profileImage);
    }

    // 유저 저장(바로 role 부여)
    userRepository.save(User.builder()
        .email(request.getEmail())
        .nickname(request.getNickname())
        .password(passwordEncoder.encode(request.getPassword()))
        .profileImage(profileImageUrl)
        .status(ACTIVE)
        .role(ROLE_USER)
        .build());
  }

  // 사용자 로그인
  @Transactional
  public LoginResponse login(@Valid LoginRequest request) {

    // 사용자 조회
    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    // 비밀번호 검증
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new MeongnyangerangException(INVALID_PASSWORD);
    }

    // 사용자 상태 검증
    if (user.getStatus() == DELETED) {
      throw new MeongnyangerangException(ACCOUNT_DELETED);
    }

    // Access Token 발급
    String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(),
        user.getRole().name(), user.getStatus());

    // Refresh Token 발급
    String refreshToken = jwtTokenProvider.createRefreshToken();

    // Refresh Token 저장
    refreshTokenRepository.deleteByUserId(user.getId()); // 중복 방지
    refreshTokenRepository.save(RefreshToken.builder()
        .refreshToken(refreshToken)
        .userId(user.getId())
        .role(user.getRole())
        .expiryDate(LocalDateTime.now().plusDays(7))
        .build());

    // Access Token + Refresh Token 함께 응답
    return new LoginResponse(accessToken, refreshToken);
  }

  // 사용자 회원 탈퇴
  @Transactional
  public void deleteUser(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    // 예약 상태 확인
    if (reservationRepository.existsByUserIdAndStatus(userId, ReservationStatus.RESERVED)) {
      throw new MeongnyangerangException(RESERVED_RESERVATION_EXISTS);
    }

    user.setStatus(DELETED);
    user.setDeletedAt(LocalDateTime.now());
  }

  // 사용자 프로필 조회
  @Transactional(readOnly = true)
  public UserProfileResponse getMyProfile(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    return UserProfileResponse.of(user);
  }

  // 사용자 비밀번호 변경
  @Transactional
  public void updatePassword(Long userId, PasswordUpdateRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
      throw new MeongnyangerangException(INVALID_PASSWORD);
    }
    user.updatePassword(passwordEncoder.encode(request.newPassword()));
  }

  // 사용자 닉네임 변경
  @Transactional
  public void updateNickname(Long userId, String newNickname) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    if (user.getNickname().equals(newNickname)) {
      throw new MeongnyangerangException(ALREADY_REGISTERED_NICKNAME);
    }

    // 이메일 중복 확인 및 예외처리
    authService.checkNickname(newNickname);

    user.updateNickname(newNickname);
  }

  // 사용자 프로필 사진 변경
  @Transactional
  public void updateProfileImage(Long userId, MultipartFile newProfileImage) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new MeongnyangerangException(NOT_EXIST_ACCOUNT));

    if (user.getProfileImage() != null && !user.getProfileImage().isBlank()) {
      imageService.deleteImageAsync(user.getProfileImage());
    }
    user.updateProfileImage(imageService.storeImage(newProfileImage));
  }
}
