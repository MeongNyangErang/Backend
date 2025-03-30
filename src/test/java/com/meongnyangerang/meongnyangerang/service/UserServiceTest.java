package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.UserSignupRequest;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.UserRepository;
import com.meongnyangerang.meongnyangerang.service.image.ImageService;
import java.io.IOException;
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

  @Test
  @DisplayName("사용자 회원가입 성공 테스트 - 프로필 이미지 없음")
  void registerUserSuccessWithoutImage() {
    // given
    UserSignupRequest request = new UserSignupRequest();
    request.setEmail("user@example.com");
    request.setNickname("nickname");
    request.setPassword("password123!");

    when(userRepository.existsByEmail(any())).thenReturn(false);
    when(passwordEncoder.encode(any())).thenReturn("encodedPassword");

    // when & then
    assertDoesNotThrow(() -> userService.registerUser(request, null));
    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("사용자 회원가입 성공 테스트 - 프로필 이미지 포함")
  void registerUserSuccessWithImage() throws IOException {
    // given
    UserSignupRequest request = new UserSignupRequest();
    request.setEmail("user@example.com");
    request.setNickname("nickname");
    request.setPassword("password123!");

    MockMultipartFile imageFile = new MockMultipartFile(
        "profileImage", "test.png", "image/png", "dummy".getBytes()
    );

    when(userRepository.existsByEmail(any())).thenReturn(false);
    when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
    when(imageService.storeImage(any())).thenReturn("https://s3.bucket/image/test.png");

    // when & then
    assertDoesNotThrow(() -> userService.registerUser(request, imageFile));
    verify(userRepository).save(any(User.class));
  }
}
