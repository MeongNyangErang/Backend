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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @InjectMocks
  private UserService userService;

  @Mock
  private UserRepository userRepository;

  @Test
  @DisplayName("사용자 회원가입 성공 테스트")
  void registerUserSuccess() {
    // given
    UserSignupRequest request = new UserSignupRequest();
    request.setEmail("user@example.com");
    when(userRepository.existsByEmail(any())).thenReturn(false);

    // when & then
    assertDoesNotThrow(() -> userService.registerUser(request));
    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("중복 이메일 회원가입 실패 테스트")
  void registerUserDuplicateEmail() {
    // given
    when(userRepository.existsByEmail(any())).thenReturn(true);

    // when & then
    assertThrows(MeongnyangerangException.class, () -> {
      userService.registerUser(new UserSignupRequest());
    });
  }
}
