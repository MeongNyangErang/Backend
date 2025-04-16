package com.meongnyangerang.meongnyangerang.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.UserStatus;
import com.meongnyangerang.meongnyangerang.dto.PasswordUpdateRequest;
import com.meongnyangerang.meongnyangerang.dto.UserProfileResponse;
import com.meongnyangerang.meongnyangerang.dto.UserSignupRequest;
import com.meongnyangerang.meongnyangerang.exception.ErrorCode;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.ReservationRepository;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
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

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @InjectMocks
  private UserService userService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private ImageService imageService;

  @Mock
  private ReservationRepository reservationRepository;

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
    when(reservationRepository.existsByUserIdAndStatus(userId, ReservationStatus.RESERVED)).thenReturn(false);

    // when
    userService.deleteUser(userId);

    // then
    assertEquals(UserStatus.DELETED, user.getStatus());
    assertNotNull(user.getDeletedAt());
  }

  @Test
  @DisplayName("사용자 탈퇴 실패 - 예약 존재")
  void deleteUserFailDueToReservation() {
    // given
    Long userId = 1L;

    when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
    when(reservationRepository.existsByUserIdAndStatus(userId, ReservationStatus.RESERVED)).thenReturn(true);

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
        .isEqualTo(ErrorCode.NOT_EXIST_ACCOUNT);
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
        .isEqualTo(ErrorCode.NOT_EXIST_ACCOUNT);
  }
}
