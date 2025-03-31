package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.reservation.ReservationStatus;
import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.domain.user.UserStatus;
import com.meongnyangerang.meongnyangerang.dto.UserSignupRequest;
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
}
