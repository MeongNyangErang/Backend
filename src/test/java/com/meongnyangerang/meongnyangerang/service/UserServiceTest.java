package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ALREADY_REGISTERED_NICKNAME;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.DUPLICATE_NICKNAME;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.INVALID_PASSWORD;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXIST_ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.user.AuthProvider;
import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.UserStatus;
import com.meongnyangerang.meongnyangerang.dto.LoginResponse;
import com.meongnyangerang.meongnyangerang.dto.PasswordUpdateRequest;
import com.meongnyangerang.meongnyangerang.dto.UserProfileResponse;
import com.meongnyangerang.meongnyangerang.dto.UserSignupRequest;
import com.meongnyangerang.meongnyangerang.dto.auth.KakaoUserInfoResponse;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.jwt.JwtTokenProvider;
import com.meongnyangerang.meongnyangerang.repository.ReservationRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.repository.auth.RefreshTokenRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @InjectMocks
  private UserService userService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private ImageService imageService;

  @Mock
  private ReservationRepository reservationRepository;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private AuthService authService;

  @Test
  @DisplayName("사용자 회원가입 성공 테스트 - 프로필 이미지 없음")
  void registerUserSuccessWithoutImage() {
    // given
    String email = "user@example.com";
    String nickname = "nickname";
    String password = "password123!";
    String encodedPassword = "encodedPassword";

    UserSignupRequest request = new UserSignupRequest();
    request.setEmail(email);
    request.setNickname(nickname);
    request.setPassword(password);

    when(userRepository.existsByEmail(email)).thenReturn(false);
    when(passwordEncoder.encode(password)).thenReturn(encodedPassword);

    // when & then
    assertDoesNotThrow(() -> userService.registerUser(request, null));
    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("사용자 회원가입 성공 테스트 - 프로필 이미지 포함")
  void registerUserSuccessWithImage() throws IOException {
    // given
    String email = "user@example.com";
    String nickname = "nickname";
    String password = "password123!";
    String encodedPassword = "encodedPassword";
    String imageUrl = "https://s3.bucket/image/test.png";

    UserSignupRequest request = new UserSignupRequest();
    request.setEmail(email);
    request.setNickname(nickname);
    request.setPassword(password);

    MockMultipartFile imageFile = new MockMultipartFile(
        "profileImage", "test.png", "image/png", "dummy".getBytes()
    );

    when(userRepository.existsByEmail(email)).thenReturn(false);
    when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
    when(imageService.storeImage(imageFile)).thenReturn(imageUrl);

    // when & then
    assertDoesNotThrow(() -> userService.registerUser(request, imageFile));
    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("중복 이메일 회원가입 실패 테스트")
  void registerUserDuplicateEmail() {
    // given
    String email = "duplicate@example.com";

    UserSignupRequest request = new UserSignupRequest();
    request.setEmail(email);

    when(userRepository.existsByEmail(email)).thenReturn(true);

    // when & then
    assertThrows(MeongnyangerangException.class,
        () -> userService.registerUser(request, null));
  }

  @Test
  @DisplayName("사용자 탈퇴 성공")
  void deleteUserSuccess() {
    // given
    Long userId = 1L;
    User user = User.builder()
        .id(userId)
        .email("user@example.com")
        .status(UserStatus.ACTIVE)
        .build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(reservationRepository.existsByUserIdAndStatus(userId,
        ReservationStatus.RESERVED)).thenReturn(false);

    // when
    userService.deleteUser(userId);

    // then
    assertEquals(UserStatus.DELETED, user.getStatus());
    assertNotNull(user.getDeletedAt());
  }

  // 정상 로그인 (기존 유저, ACTIVE, provider=KAKAO, oauthId 동일)
  @Test
  @DisplayName("카카오 로그인 성공 - 기존 활성화 유저가 정상적으로 토큰을 발급받는다")
  void loginWithKakao_shouldReturnToken_whenUserExistsAndValid() {
    String email = "test@example.com";
    String oauthId = "123456";
    User user = User.builder()
        .id(1L)
        .email(email)
        .nickname("닉네임")
        .provider(AuthProvider.KAKAO)
        .oauthId(oauthId)
        .role(Role.ROLE_USER)
        .status(UserStatus.ACTIVE)
        .build();

    KakaoUserInfoResponse kakaoUser = mockKakaoUser(email, oauthId, "닉네임", "http://img.com");

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name(), user.getStatus())).thenReturn("access");
    when(jwtTokenProvider.createRefreshToken()).thenReturn("refresh");

    LoginResponse response = userService.loginWithKakao(kakaoUser);

    assertEquals("access", response.getAccessToken());
    assertEquals("refresh", response.getRefreshToken());
    verify(refreshTokenRepository).deleteByUserIdAndRole(1L, Role.ROLE_USER);
    verify(refreshTokenRepository).save(any());
  }

  // 신규 유저
  @Test
  @DisplayName("카카오 로그인 성공 - 신규 유저가 회원가입 후 토큰을 발급받는다")
  void loginWithKakao_shouldRegisterAndReturnToken_whenUserNotExists() {
    String email = "new@example.com";
    KakaoUserInfoResponse kakaoUser = mockKakaoUser(email, "777", "뉴유저", "img");

    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User u = invocation.getArgument(0);
      u.setId(42L);
      return u;
    });
    when(jwtTokenProvider.createAccessToken(42L, email, Role.ROLE_USER.name(), UserStatus.ACTIVE)).thenReturn("access-new");
    when(jwtTokenProvider.createRefreshToken()).thenReturn("refresh-new");

    LoginResponse response = userService.loginWithKakao(kakaoUser);

    assertEquals("access-new", response.getAccessToken());
    assertEquals("refresh-new", response.getRefreshToken());
    verify(refreshTokenRepository).deleteByUserIdAndRole(42L, Role.ROLE_USER);
    verify(refreshTokenRepository).save(any());
  }

  private KakaoUserInfoResponse mockKakaoUser(String email, String id, String nickname, String imageUrl) {
    KakaoUserInfoResponse response = new KakaoUserInfoResponse();
    response.setId(Long.parseLong(id));

    KakaoUserInfoResponse.KakaoAccount.Profile profile = new KakaoUserInfoResponse.KakaoAccount.Profile();
    profile.setNickname(nickname);
    profile.setProfileImageUrl(imageUrl);

    KakaoUserInfoResponse.KakaoAccount account = new KakaoUserInfoResponse.KakaoAccount();
    account.setEmail(email);
    account.setProfile(profile);

    response.setKakaoAccount(account);
    return response;
  }

  @Test
  @DisplayName("사용자 탈퇴 실패 - 예약 존재")
  void deleteUserFailDueToReservation() {
    // given
    Long userId = 1L;

    when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
    when(reservationRepository.existsByUserIdAndStatus(userId,
        ReservationStatus.RESERVED)).thenReturn(true);

    // when & then
    assertThrows(MeongnyangerangException.class, () -> userService.deleteUser(userId));
  }

  @Test
  @DisplayName("사용자 프로필 조회 - 성공")
  void getUserProfile_Success() {
    // given
    Long userId = 1L;
    User user = User.builder()
        .id(userId)
        .nickname("멍냥이")
        .profileImage("https://profile.jpg")
        .build();

    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    // when
    UserProfileResponse response = userService.getMyProfile(userId);

    // then
    assertThat(response.getNickname()).isEqualTo("멍냥이");
    assertThat(response.getProfileImageUrl()).isEqualTo("https://profile.jpg");
  }

  @Test
  @DisplayName("사용자 프로필 조회 - 실패 (존재하지 않는 사용자)")
  void getUserProfile_Fail_UserNotFound() {
    // given
    Long invalidUserId = 999L;
    given(userRepository.findById(invalidUserId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.getMyProfile(invalidUserId))
        .isInstanceOf(MeongnyangerangException.class)
        .extracting("errorCode")
        .isEqualTo(NOT_EXIST_ACCOUNT);
  }

  @Test
  @DisplayName("사용자 비밀번호 변경 - 성공")
  void updatePassword_Success() {
    // given
    Long userId = 1L;
    User user = User.builder()
        .id(userId)
        .password("encodedOldPassword")
        .build();

    PasswordUpdateRequest request = new PasswordUpdateRequest("oldPassword", "newPassword1!");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(passwordEncoder.matches("oldPassword", "encodedOldPassword")).willReturn(true);
    given(passwordEncoder.encode("newPassword1!")).willReturn("encodedNewPassword");

    // when
    userService.updatePassword(userId, request);

    // then
    assertThat(user.getPassword()).isEqualTo("encodedNewPassword");
  }

  @Test
  @DisplayName("사용자 비밀번호 변경 - 실패(존재하지 않는 사용자)")
  void updatePassword_Fail_NotExistUser() {
    // given
    Long userId = 1L;
    PasswordUpdateRequest request = new PasswordUpdateRequest("oldPassword", "newPassword1!");

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.updatePassword(userId, request))
        .isInstanceOf(MeongnyangerangException.class)
        .extracting("errorCode")
        .isEqualTo(NOT_EXIST_ACCOUNT);
  }

  @Test
  @DisplayName("사용자 비밀번호 변경 - 실패(기존 비밀번호 불일치)")
  void updatePassword_Fail_InvalidPassword() {
    // given
    Long userId = 1L;
    User user = User.builder()
        .id(userId)
        .password("encodedOldPassword")
        .build();

    PasswordUpdateRequest request = new PasswordUpdateRequest("wrongOldPassword", "newPassword1!");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(passwordEncoder.matches("wrongOldPassword", "encodedOldPassword")).willReturn(false);

    // when & then
    assertThatThrownBy(() -> userService.updatePassword(userId, request))
        .isInstanceOf(MeongnyangerangException.class)
        .extracting("errorCode")
        .isEqualTo(INVALID_PASSWORD);
  }


  @Test
  @DisplayName("사용자 닉네임 변경 - 성공")
  void updateNickname_Success() {
    // given
    Long userId = 1L;
    User user = User.builder()
        .id(userId)
        .nickname("oldNickname")
        .build();

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    willDoNothing().given(authService).checkNickname("newNickname");

    // when
    userService.updateNickname(userId, "newNickname");

    // then
    assertThat(user.getNickname()).isEqualTo("newNickname");
  }

  @DisplayName("사용자 닉네임 변경 - 실패 (이미 등록된 닉네임)")
  @Test
  void updateNickname_User_AlreadyRegistered() {
    // given
    Long userId = 1L;
    User user = User.builder()
        .id(userId)
        .nickname("sameNick")
        .build();

    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    // when & then
    assertThatThrownBy(() -> userService.updateNickname(userId, "sameNick"))
        .isInstanceOf(MeongnyangerangException.class)
        .extracting("errorCode")
        .isEqualTo(ALREADY_REGISTERED_NICKNAME);
  }

  @DisplayName("사용자 닉네임 변경 - 실패 (중복 닉네임 존재)")
  @Test
  void updateNickname_User_DuplicateNickname() {
    // given
    Long userId = 1L;
    User user = User.builder()
        .id(userId)
        .nickname("oldNick")
        .build();

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    willThrow(new MeongnyangerangException(DUPLICATE_NICKNAME)).given(authService)
        .checkNickname("duplicateNick");

    // when & then
    assertThatThrownBy(() -> userService.updateNickname(userId, "duplicateNick"))
        .isInstanceOf(MeongnyangerangException.class)
        .extracting("errorCode")
        .isEqualTo(DUPLICATE_NICKNAME);
  }

  @Test
  @DisplayName("사용자 프로필 이미지 변경 - 성공 (기존 이미지 O)")
  void updateProfileImage_success_withExistingImage() {
    // given
    Long userId = 1L;
    MultipartFile newImage = mock(MultipartFile.class);
    User user = User.builder()
        .id(userId)
        .profileImage("https://s3.aws/old-image.jpg")
        .build();

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(imageService.storeImage(newImage)).willReturn("https://s3.aws/new-image.jpg");

    // when
    userService.updateProfileImage(userId, newImage);

    // then
    verify(imageService).deleteImageAsync("https://s3.aws/old-image.jpg");
    assertThat(user.getProfileImage()).isEqualTo("https://s3.aws/new-image.jpg");
  }

  @Test
  @DisplayName("사용자 프로필 이미지 변경 - 성공 (기존 이미지 X)")
  void updateProfileImage_success_withoutExistingImage() {
    Long userId = 2L;
    MultipartFile newImage = mock(MultipartFile.class);
    User user = User.builder()
        .id(userId)
        .profileImage(null)
        .build();

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(imageService.storeImage(newImage)).willReturn("https://s3.aws/new-image.jpg");

    userService.updateProfileImage(userId, newImage);

    verify(imageService, never()).deleteImageAsync(any());
    assertThat(user.getProfileImage()).isEqualTo("https://s3.aws/new-image.jpg");
  }

  @Test
  @DisplayName("사용자 프로필 이미지 변경 - 실패 (존재하지 않는 사용자)")
  void updateProfileImage_fail_userNotFound() {
    Long userId = 999L;
    MultipartFile newImage = mock(MultipartFile.class);

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> userService.updateProfileImage(userId, newImage))
        .isInstanceOf(MeongnyangerangException.class)
        .extracting("errorCode")
        .isEqualTo(NOT_EXIST_ACCOUNT);
  }
}
