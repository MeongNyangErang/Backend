package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.user.User;
import com.meongnyangerang.meongnyangerang.dto.UserSignupRequest;
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


}
