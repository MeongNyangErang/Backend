package com.meongnyangerang.meongnyangerang.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meongnyangerang.meongnyangerang.domain.host.Host;
import com.meongnyangerang.meongnyangerang.dto.HostSignupRequest;
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
}
