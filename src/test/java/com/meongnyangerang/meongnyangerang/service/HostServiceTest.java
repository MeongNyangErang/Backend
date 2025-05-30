package com.meongnyangerang.meongnyangerang.service;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ALREADY_REGISTERED_NAME;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.ALREADY_REGISTERED_NICKNAME;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.DUPLICATE_NICKNAME;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.INVALID_AUTHORIZED;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.INVALID_PASSWORD;
import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.NOT_EXIST_ACCOUNT;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.auth.RefreshToken;
import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.domain.host.HostStatus;
import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.user.Role;
import com.meongnyangerang.meongnyangerang.dto.HostProfileResponse;
import com.meongnyangerang.meongnyangerang.dto.HostSignupRequest;
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
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class HostServiceTest {

  @InjectMocks
  private HostService hostService;

  @Mock
  private HostRepository hostRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private ImageService imageService;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private ReservationRepository reservationRepository;

  @Mock
  private AuthService authService;

  @Test
  @DisplayName("호스트 회원가입 성공 테스트")
  void registerHostSuccess() throws IOException {
    // given
    String email = "host@example.com";
    String password = "password123";
    String encodedPassword = "encodedPassword";

    HostSignupRequest request = new HostSignupRequest();
    request.setEmail(email);
    request.setName("호스트");
    request.setNickname("호스트닉네임");
    request.setPassword(password);
    request.setPhoneNumber("010-1234-5678");

    MultipartFile profileImage = new MockMultipartFile("profile", "profile.jpg", "image/jpeg",
        new byte[0]);
    MultipartFile businessLicense = new MockMultipartFile("license", "license.jpg", "image/jpeg",
        new byte[0]);
    MultipartFile submitDocument = new MockMultipartFile("document", "document.jpg", "image/jpeg",
        new byte[0]);

    when(hostRepository.existsByEmail(email)).thenReturn(false);
    when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
    when(imageService.storeImage(profileImage)).thenReturn("http://s3.com/profile.jpg");
    when(imageService.storeImage(businessLicense)).thenReturn("http://s3.com/license.jpg");
    when(imageService.storeImage(submitDocument)).thenReturn("http://s3.com/document.jpg");

    // when
    assertDoesNotThrow(
        () -> hostService.registerHost(request, profileImage, businessLicense, submitDocument));

    // then
    verify(hostRepository).save(any(Host.class));
  }

  @Test
  @DisplayName("중복 이메일 호스트 회원가입 실패 테스트")
  void registerHostDuplicateEmail() {
    // given
    String email = "existing@example.com";

    HostSignupRequest request = new HostSignupRequest();
    request.setEmail(email);

    when(hostRepository.existsByEmail(email)).thenReturn(true);

    // when & then
    assertThrows(MeongnyangerangException.class,
        () -> hostService.registerHost(request, null, null, null));
    verify(hostRepository, never()).save(any(Host.class));
  }

  @Test
  @DisplayName("카카오 로그인 성공 - 상태 ACTIVE(기존 회원가입 완료)")
  void loginWithKakao_success() {
    // given
    String email = "host@example.com";
    Long kakaoId = 98765L;

    KakaoUserInfoResponse kakaoUser = createKakaoUserInfo(email, kakaoId);
    Host host = Host.builder()
        .id(1L)
        .email(email)
        .role(Role.ROLE_HOST)
        .status(HostStatus.ACTIVE)
        .build();

    given(hostRepository.findByEmail(email)).willReturn(Optional.of(host));
    given(jwtTokenProvider.createAccessToken(1L, email, "ROLE_HOST", HostStatus.ACTIVE)).willReturn("access-token");
    given(jwtTokenProvider.createRefreshToken()).willReturn("refresh-token");

    // when
    LoginResponse response = hostService.loginWithKakao(kakaoUser);

    // then
    assertThat(response.getAccessToken()).isEqualTo("access-token");
    assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

    then(refreshTokenRepository).should().deleteByUserIdAndRole(1L, Role.ROLE_HOST);
    then(refreshTokenRepository).should().save(any(RefreshToken.class));
  }

  @Test
  @DisplayName("카카오 로그인 실패 - 상태 PENDING(DELETED 상태도 검증 같음")
  void loginWithKakao_pendingHost() {
    // given
    String email = "host@example.com";
    KakaoUserInfoResponse kakaoUser = createKakaoUserInfo(email, 1L);
    Host host = Host.builder()
        .email(email)
        .status(HostStatus.PENDING)
        .build();

    given(hostRepository.findByEmail(email)).willReturn(Optional.of(host));

    // when & then
    assertThatThrownBy(() -> hostService.loginWithKakao(kakaoUser))
        .isInstanceOf(MeongnyangerangException.class)
        .hasMessageContaining(INVALID_AUTHORIZED.getDescription());
  }

  @Test
  @DisplayName("카카오 로그인 실패 - 호스트 존재하지 않음")
  void loginWithKakao_hostNotFound() {
    // given
    String email = "host@example.com";
    KakaoUserInfoResponse kakaoUser = createKakaoUserInfo(email, 1L);

    given(hostRepository.findByEmail(email)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> hostService.loginWithKakao(kakaoUser))
        .isInstanceOf(MeongnyangerangException.class)
        .hasMessageContaining(NOT_EXIST_ACCOUNT.getDescription());
  }

  private KakaoUserInfoResponse createKakaoUserInfo(String email, Long kakaoId) {
    KakaoUserInfoResponse.KakaoAccount.Profile profile = new KakaoUserInfoResponse.KakaoAccount.Profile();
    ReflectionTestUtils.setField(profile, "nickname", "호스트");
    ReflectionTestUtils.setField(profile, "profileImageUrl", "https://example.com/profile.jpg");

    KakaoUserInfoResponse.KakaoAccount account = new KakaoUserInfoResponse.KakaoAccount();
    ReflectionTestUtils.setField(account, "email", email);
    ReflectionTestUtils.setField(account, "profile", profile);

    KakaoUserInfoResponse userInfo = new KakaoUserInfoResponse();
    ReflectionTestUtils.setField(userInfo, "id", kakaoId);
    ReflectionTestUtils.setField(userInfo, "kakaoAccount", account);
    return userInfo;
  }

  @Test
  @DisplayName("호스트 탈퇴 성공")
  void deleteHostSuccess() {
    // given
    Long hostId = 1L;

    Host host = Host.builder()
        .id(hostId)
        .email("host@example.com")
        .status(HostStatus.ACTIVE)
        .build();

    when(hostRepository.findById(hostId)).thenReturn(Optional.of(host));
    when(reservationRepository.existsByHostIdAndStatus(hostId,
        ReservationStatus.RESERVED)).thenReturn(false);

    // when
    hostService.deleteHost(hostId);

    // then
    assertEquals(HostStatus.DELETED, host.getStatus());
    assertNotNull(host.getDeletedAt());
  }

  @Test
  @DisplayName("호스트 탈퇴 실패 - 예약 존재")
  void deleteHostFailDueToReservation() {
    // given
    Long hostId = 1L;

    when(hostRepository.findById(hostId)).thenReturn(Optional.of(new Host()));
    when(reservationRepository.existsByHostIdAndStatus(hostId,
        ReservationStatus.RESERVED)).thenReturn(true);

    // when & then
    assertThrows(MeongnyangerangException.class, () -> hostService.deleteHost(hostId));
  }

  @Test
  @DisplayName("호스트 프로필 조회 - 성공")
  void getHostProfile_Success() {
    // given
    Long hostId = 1L;

    Host host = Host.builder()
        .id(hostId)
        .name("홍길동")
        .nickname("길동이")
        .phoneNumber("010-1234-5678")
        .profileImageUrl("https://example.com/image.jpg")
        .build();

    Mockito.when(hostRepository.findById(hostId)).thenReturn(Optional.of(host));

    // when
    HostProfileResponse response = hostService.getHostProfile(hostId);

    // then
    assertThat(response.getName()).isEqualTo("홍길동");
    assertThat(response.getNickname()).isEqualTo("길동이");
    assertThat(response.getPhone()).isEqualTo("010-1234-5678");
    assertThat(response.getProfileImageUrl()).isEqualTo("https://example.com/image.jpg");
  }

  @Test
  @DisplayName("호스트 프로필 조회 - 실패 (존재하지 않는 계정)")
  void getHostProfile_Fail_NotFound() {
    // given
    Long hostId = 999L;
    Mockito.when(hostRepository.findById(hostId)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> hostService.getHostProfile(hostId))
        .isInstanceOf(MeongnyangerangException.class)
        .hasFieldOrPropertyWithValue("errorCode", NOT_EXIST_ACCOUNT);
  }

  @Test
  @DisplayName("호스트 전화번호 변경 - 성공")
  void updatePhoneNumber_Success() {
    // given
    Long hostId = 1L;
    String originalPhone = "010-1234-5678";
    String newPhone = "010-9999-8888";

    Host host = Host.builder()
        .id(hostId)
        .phoneNumber(originalPhone)
        .build();

    given(hostRepository.findById(hostId)).willReturn(Optional.of(host));
    given(hostRepository.existsByPhoneNumberAndIdNot(newPhone, hostId)).willReturn(false);

    // when
    hostService.updatePhoneNumber(hostId, newPhone);

    // then
    assertThat(host.getPhoneNumber()).isEqualTo(newPhone);
  }

  @Test
  @DisplayName("호스트 전화번호 변경 - 실패 (존재하지 않는 호스트)")
  void updatePhoneNumber_Fail_NotExist() {
    // given
    Long hostId = 1L;
    String phone = "010-1234-5678";

    given(hostRepository.findById(hostId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> hostService.updatePhoneNumber(hostId, phone))
        .isInstanceOf(MeongnyangerangException.class)
        .extracting("errorCode")
        .isEqualTo(NOT_EXIST_ACCOUNT);
  }

  @Test
  @DisplayName("호스트 전화번호 변경 - 실패 (기존 전화번호와 동일)")
  void updatePhoneNumber_Fail_SamePhone() {
    // given
    Long hostId = 1L;
    String phone = "010-1234-5678";

    Host host = Host.builder()
        .id(hostId)
        .phoneNumber(phone)
        .build();

    given(hostRepository.findById(hostId)).willReturn(Optional.of(host));

    // when & then
    assertThatThrownBy(() -> hostService.updatePhoneNumber(hostId, phone))
        .isInstanceOf(MeongnyangerangException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ALREADY_REGISTERED_PHONE_NUMBER);
  }

  @Test
  @DisplayName("호스트 전화번호 변경 - 실패 (중복된 전화번호)")
  void updatePhoneNumber_Fail_DuplicatePhone() {
    // given
    Long hostId = 1L;
    String oldPhone = "010-1234-5678";
    String newPhone = "010-8888-9999";

    Host host = Host.builder()
        .id(hostId)
        .phoneNumber(oldPhone)
        .build();

    given(hostRepository.findById(hostId)).willReturn(Optional.of(host));
    given(hostRepository.existsByPhoneNumberAndIdNot(newPhone, hostId)).willReturn(true);

    // when & then
    assertThatThrownBy(() -> hostService.updatePhoneNumber(hostId, newPhone))
        .isInstanceOf(MeongnyangerangException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DUPLICATE_PHONE_NUMBER);
  }

  @DisplayName("호스트 이름 변경 - 성공")
  @Test
  void updateName_success() {
    // given
    Long hostId = 1L;
    Host host = Host.builder()
        .id(hostId)
        .name("기존이름")
        .build();

    given(hostRepository.findById(hostId)).willReturn(Optional.of(host));

    // when
    hostService.updateName(hostId, "새이름");

    // then
    assertThat(host.getName()).isEqualTo("새이름");
  }

  @DisplayName("호스트 이름 변경 - 실패 (동일한 이름)")
  @Test
  void updateName_fail_sameName() {
    // given
    Long hostId = 1L;
    String sameName = "동일이름";
    Host host = Host.builder()
        .id(hostId)
        .name(sameName)
        .build();

    given(hostRepository.findById(hostId)).willReturn(Optional.of(host));

    // when & then
    assertThatThrownBy(() -> hostService.updateName(hostId, sameName))
        .isInstanceOf(MeongnyangerangException.class)
        .extracting("errorCode")
        .isEqualTo(ALREADY_REGISTERED_NAME);
  }

  @DisplayName("호스트 이름 변경 - 실패 (존재하지 않는 호스트)")
  @Test
  void updateName_fail_hostNotFound() {
    // given
    Long hostId = 1L;
    given(hostRepository.findById(hostId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> hostService.updateName(hostId, "아무이름"))
        .isInstanceOf(MeongnyangerangException.class)
        .extracting("errorCode")
        .isEqualTo(NOT_EXIST_ACCOUNT);
  }

  @Test
  @DisplayName("호스트 비밀번호 변경 - 성공")
  void updatePassword_Success() {
    // given
    Long hostId = 1L;
    Host host = Host.builder()
        .id(hostId)
        .password("encodedOldPassword")
        .build();

    PasswordUpdateRequest request = new PasswordUpdateRequest("oldPassword", "newPassword1!");

    given(hostRepository.findById(hostId)).willReturn(Optional.of(host));
    given(passwordEncoder.matches("oldPassword", "encodedOldPassword")).willReturn(true);
    given(passwordEncoder.encode("newPassword1!"))
        .willReturn("encodedNewPassword");

    // when
    hostService.updatePassword(hostId, request);

    // then
    assertThat(host.getPassword()).isEqualTo("encodedNewPassword");
  }

  @Test
  @DisplayName("호스트 비밀번호 변경 - 실패(존재하지 않는 호스트)")
  void updatePassword_Fail_NotExistHost() {
    // given
    Long hostId = 1L;
    PasswordUpdateRequest request = new PasswordUpdateRequest("oldPassword", "newPassword1!");

    given(hostRepository.findById(hostId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> hostService.updatePassword(hostId, request))
        .isInstanceOf(MeongnyangerangException.class)
        .extracting("errorCode")
        .isEqualTo(NOT_EXIST_ACCOUNT);
  }

  @Test
  @DisplayName("호스트 비밀번호 변경 - 실패(기존 비밀번호 불일치)")
  void updatePassword_Fail_InvalidPassword() {
    // given
    Long hostId = 1L;
    Host host = Host.builder()
        .id(hostId)
        .password("encodedOldPassword")
        .build();

    PasswordUpdateRequest request = new PasswordUpdateRequest("wrongOldPassword", "newPassword1!");

    given(hostRepository.findById(hostId)).willReturn(Optional.of(host));
    given(passwordEncoder.matches("wrongOldPassword", "encodedOldPassword")).willReturn(false);

    // when & then
    assertThatThrownBy(() -> hostService.updatePassword(hostId, request))
        .isInstanceOf(MeongnyangerangException.class)
        .extracting("errorCode")
        .isEqualTo(INVALID_PASSWORD);
  }

  @DisplayName("호스트 닉네임 변경 - 성공")
  @Test
  void updateNickname_Host_Success() {
    // given
    Long hostId = 1L;
    Host host = Host.builder()
        .id(hostId)
        .nickname("oldNick")
        .build();

    given(hostRepository.findById(hostId)).willReturn(Optional.of(host));
    willDoNothing().given(authService).checkNickname("newNick");

    // when
    hostService.updateNickname(hostId, "newNick");

    // then
    assertThat(host.getNickname()).isEqualTo("newNick");
  }

  @DisplayName("호스트 닉네임 변경 - 실패 (이미 등록된 닉네임)")
  @Test
  void updateNickname_Host_AlreadyRegistered() {
    // given
    Long hostId = 1L;
    Host host = Host.builder()
        .id(hostId)
        .nickname("sameNick")
        .build();

    given(hostRepository.findById(hostId)).willReturn(Optional.of(host));

    // when & then
    assertThatThrownBy(() -> hostService.updateNickname(hostId, "sameNick"))
        .isInstanceOf(MeongnyangerangException.class)
        .extracting("errorCode")
        .isEqualTo(ALREADY_REGISTERED_NICKNAME);
  }

  @DisplayName("호스트 닉네임 변경 - 실패 (중복 닉네임 존재)")
  @Test
  void updateNickname_Host_DuplicateNickname() {
    // given
    Long hostId = 1L;
    Host host = Host.builder()
        .id(hostId)
        .nickname("oldNick")
        .build();

    given(hostRepository.findById(hostId)).willReturn(Optional.of(host));
    willThrow(new MeongnyangerangException(DUPLICATE_NICKNAME)).given(authService)
        .checkNickname("duplicateNick");

    // when & then
    assertThatThrownBy(() -> hostService.updateNickname(hostId, "duplicateNick"))
        .isInstanceOf(MeongnyangerangException.class)
        .extracting("errorCode")
        .isEqualTo(DUPLICATE_NICKNAME);
  }

  @Test
  @DisplayName("호스트 프로필 이미지 변경 - 성공 (기존 이미지 O)")
  void updateProfileImage_success_withExistingImage() {
    Long hostId = 1L;
    MultipartFile newImage = mock(MultipartFile.class);
    Host host = Host.builder()
        .id(hostId)
        .profileImageUrl("https://s3.aws/old-host.jpg")
        .build();

    given(hostRepository.findById(hostId)).willReturn(Optional.of(host));
    given(imageService.storeImage(newImage)).willReturn("https://s3.aws/new-host.jpg");

    hostService.updateProfileImage(hostId, newImage);

    verify(imageService).deleteImageAsync("https://s3.aws/old-host.jpg");
    assertThat(host.getProfileImageUrl()).isEqualTo("https://s3.aws/new-host.jpg");
  }

  @Test
  @DisplayName("호스트 프로필 이미지 변경 - 성공 (기존 이미지 X)")
  void updateProfileImage_success_withoutExistingImage() {
    Long hostId = 2L;
    MultipartFile newImage = mock(MultipartFile.class);
    Host host = Host.builder()
        .id(hostId)
        .profileImageUrl(null)
        .build();

    given(hostRepository.findById(hostId)).willReturn(Optional.of(host));
    given(imageService.storeImage(newImage)).willReturn("https://s3.aws/new-host.jpg");

    hostService.updateProfileImage(hostId, newImage);

    verify(imageService, never()).deleteImageAsync(any());
    assertThat(host.getProfileImageUrl()).isEqualTo("https://s3.aws/new-host.jpg");
  }

  @Test
  @DisplayName("호스트 프로필 이미지 변경 - 실패 (존재하지 않는 호스트)")
  void updateProfileImage_fail_hostNotFound() {
    Long hostId = 999L;
    MultipartFile newImage = mock(MultipartFile.class);

    given(hostRepository.findById(hostId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> hostService.updateProfileImage(hostId, newImage))
        .isInstanceOf(MeongnyangerangException.class)
        .extracting("errorCode")
        .isEqualTo(NOT_EXIST_ACCOUNT);
  }
}
