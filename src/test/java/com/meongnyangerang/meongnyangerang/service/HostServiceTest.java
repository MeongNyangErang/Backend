package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.dto.HostSignupRequest;
import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import com.meongnyangerang.meongnyangerang.repository.HostRepository;
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
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class HostServiceTest {

  @Mock
  private HostRepository hostRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private ImageService imageService;

  @InjectMocks
  private HostService hostService;

  @Test
  @DisplayName("호스트 회원가입 성공 테스트")
  void registerHostSuccess() throws IOException {
    // given
    HostSignupRequest request = new HostSignupRequest();
    request.setEmail("host@example.com");
    request.setName("호스트");
    request.setNickname("호스트닉네임");
    request.setPassword("password123");
    request.setPhoneNumber("010-1234-5678");

    MultipartFile profileImage = new MockMultipartFile("profile", "profile.jpg", "image/jpeg", new byte[0]);
    MultipartFile businessLicense = new MockMultipartFile("license", "license.jpg", "image/jpeg", new byte[0]);
    MultipartFile submitDocument = new MockMultipartFile("document", "document.jpg", "image/jpeg", new byte[0]);

    when(hostRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(imageService.storeImage(profileImage)).thenReturn("http://s3.com/profile.jpg");
    when(imageService.storeImage(businessLicense)).thenReturn("http://s3.com/license.jpg");
    when(imageService.storeImage(submitDocument)).thenReturn("http://s3.com/document.jpg");

    // when
    assertDoesNotThrow(() -> hostService.registerHost(request, profileImage, businessLicense, submitDocument));

    // then
    verify(hostRepository).save(any(Host.class));
  }

  @Test
  @DisplayName("중복 이메일 호스트 회원가입 실패 테스트")
  void registerHostDuplicateEmail() {
    // given
    HostSignupRequest request = new HostSignupRequest();
    request.setEmail("existing@example.com");

    when(hostRepository.existsByEmail(anyString())).thenReturn(true);

    // when & then
    assertThrows(MeongnyangerangException.class,
        () -> hostService.registerHost(request, null, null, null));
    verify(hostRepository, never()).save(any(Host.class));
  }
}
