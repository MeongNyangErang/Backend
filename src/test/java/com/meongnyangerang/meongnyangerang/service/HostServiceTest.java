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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HostServiceTest {

  @Mock
  private HostRepository hostRepository;

  @InjectMocks
  private HostService hostService;

  @Test
  @DisplayName("호스트 회원가입 성공 테스트")
  void registerHostSuccess() {
    // given
    HostSignupRequest request = new HostSignupRequest();
    request.setEmail("host@example.com");
    request.setName("호스트");
    request.setNickname("호스트닉네임");
    request.setPassword("password123");
    request.setBusinessLicenseImageUrl("http://example.com/license.jpg");
    request.setSubmitDocumentImageUrl("http://example.com/document.jpg");
    request.setPhoneNumber("010-1234-5678");

    when(hostRepository.existsByEmail(anyString())).thenReturn(false);

    // when
    assertDoesNotThrow(() -> hostService.registerHost(request));

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
    assertThrows(MeongnyangerangException.class, () -> hostService.registerHost(request));
    verify(hostRepository, never()).save(any(Host.class));
  }
}
